package com.example.wya;

import android.util.Log;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class muse_plus {
    // muse+: accepts real-time acc, gyroscope, and magnetometer data
    // keep updating acc and magnetometer, while every time a gyroscope data comes in,
    // update the rotation matrix, and calibrate the rotation matrix from the acc and magnetometer
    private final float lowermglimit = 40;
    private final float uppermglimit = 70;
    private final float NS2S = 1.0f / 1000000000.0f;
    private final float MS2S = 1.0f / 1000.0f;

    private final float mg_cali_alpha = 0.01f;
    private final float acc_cali_alpha = 0.01f;

    //    private float[] rotationMatrix;
    private float[] acc;
    private float[] gyro;
    private float[] mag;
    private boolean isMagAccessed;
    private float[] acc_calibrated;
    private float[] gyro_calibrated;
    private float rotate_theta;
    private long gyro_timestamp;
    private boolean initialized;

    private global_mag mGlobalMag;
    private float[] mGlobalGravity;
    public calibration mcalibration;

    private orientation_mat cur_orientation;

    /*
    implement following matrix computation functions
     */
    private float oneD3Element_Norm(@NonNull float[] vec){
        return (float) Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1] + vec[2] * vec[2]);
    }

    private float np_vector_dot(float[] a, float[] b){
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private float[] matrix_dot_vector(float[][] a, float[] b){
        float[] c = new float[3];
        for (int i = 0; i < 3; i++){
            c[i] = np_vector_dot(a[i], b);
        }
        return c;
    }
    //NOTE: verified
    private float[][] matrix_A_mul_matrix_B_T(float[][] a, float[][] b){
        float[][] c = new float[3][3];
        for (int i = 0; i < 3; i++){
            for (int j = 0; j < 3; j++){
                c[i][j] = np_vector_dot(a[i], b[j]);
            }
        }
        return c;
    }

    private float[][] matrix_A_mul_matrix_B(float[][] a, float[][] b){
        return matrix_A_mul_matrix_B_T(a, np_transpose(b));
    }

    private float vector_angle(float[] a, float[] b){
        // compute angle between two vectors, using cosine rule
        float cos_theta = np_vector_dot(a, b) / (oneD3Element_Norm(a) * oneD3Element_Norm(b));
        // constrain cos_theta to [-1, 1]
        cos_theta = Math.max(-1, Math.min(1, cos_theta));
        return (float) Math.acos(cos_theta);
    }

    private float[] np_vector_cross(float[] a, float[] b){
        float[] c = new float[3];
        c[0] = a[1] * b[2] - a[2] * b[1];
        c[1] = a[2] * b[0] - a[0] * b[2];
        c[2] = a[0] * b[1] - a[1] * b[0];
        return c;
    }

    private float[][] np_transpose(float[][] a){
        float[][] b = new float[a[0].length][a.length];
        for (int i = 0; i < a.length; i++){
            for (int j = 0; j < a[0].length; j++){
                b[j][i] = a[i][j];
            }
        }
        return b;
    }
    /*------------------------------------------------------------------------------------------ */

    private float[][] getRotationfromVector(float[] rotationVector){
        float q1 = rotationVector[0];
        float q2 = rotationVector[1];
        float q3 = rotationVector[2];
        float q0;
        // if rotationVector size larger than 4, then we have q0, otherwise compute q0
        if (rotationVector.length >= 4){
            q0 = rotationVector[3];
        }else {
            q0 = (float) Math.sqrt(1 - q1 * q1 - q2 * q2 - q3 * q3);
        }
        float sq_q1 = 2 * q1 * q1;
        float sq_q2 = 2 * q2 * q2;
        float sq_q3 = 2 * q3 * q3;
        float q1_q2 = 2 * q1 * q2;
        float q3_q0 = 2 * q3 * q0;
        float q1_q3 = 2 * q1 * q3;
        float q2_q0 = 2 * q2 * q0;
        float q2_q3 = 2 * q2 * q3;
        float q1_q0 = 2 * q1 * q0;

        float r0 = 1 - sq_q2 - sq_q3;
        float r1 = q1_q2 - q3_q0;
        float r2 = q1_q3 + q2_q0;
        float r3 = q1_q2 + q3_q0;
        float r4 = 1 - sq_q1 - sq_q3;
        float r5 = q2_q3 - q1_q0;
        float r6 = q1_q3 - q2_q0;
        float r7 = q2_q3 + q1_q0;
        float r8 = 1 - sq_q1 - sq_q2;
        return new float[][]{{r0, r1, r2},
                {r3, r4, r5},
                {r6, r7, r8}};
    }

    @Nullable
    private float[][]  getOrientationfromGravityandMag(float[] gravity, float[] geomagnetic){
        float ax = gravity[0];
        float ay = gravity[1];
        float az = gravity[2];
        float normsqA = (ax * ax + ay * ay + az * az);
        float g = 9.81f;
        float freeFallGravitySquared = 0.01f * g * g;
        if (normsqA < freeFallGravitySquared)
            return null;

        float ex = geomagnetic[0];
        float ey = geomagnetic[1];
        float ez = geomagnetic[2];

        float hx = ey * az - ez * ay;
        float hy = ez * ax - ex * az;
        float hz = ex * ay - ey * ax;
        float normH = (float) Math.sqrt(hx * hx + hy * hy + hz * hz);
        if (normH < 0.1)
            return null;
        float invH = 1.0f / normH;
        hx = invH * hx;
        hy = invH * hy;
        hz = invH * hz;
        float invA = (float) (1.0f / Math.sqrt(ax * ax + ay * ay + az * az));
        ax = invA * ax;
        ay = invA * ay;
        az = invA * az;
        float mx = ay * hz - az * hy;
        float my = az * hx - ax * hz;
        float mz = ax * hy - ay * hx;

        return new float[][]{{hx, hy, hz},
                {mx, my, mz},
                {ax, ay, az}};
    }

    @NonNull
    private float[][] deltaRfromGyro(long timestamp, float[] gyroReadings, long gyroTimestamp){
        //check if using NS2S or MS2S here, be carefule about timestamp unit
        float[] deltaRotationVector = new float[4];
        float EPSILON = 0.087f;
        if (gyroTimestamp != 0){
            final float dT = (timestamp - gyroTimestamp) * NS2S;
            float axisX = gyroReadings[0];
            float axisY = gyroReadings[1];
            float axisZ = gyroReadings[2];
            float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
            if (omegaMagnitude > EPSILON){
                axisX /= omegaMagnitude;
                axisY /= omegaMagnitude;
                axisZ /= omegaMagnitude;
            }
            float thetaOverTwo = omegaMagnitude * dT / 2.0f;
            float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
            float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
            deltaRotationVector[0] = sinThetaOverTwo * axisX;
            deltaRotationVector[1] = sinThetaOverTwo * axisY;
            deltaRotationVector[2] = sinThetaOverTwo * axisZ;
            deltaRotationVector[3] = cosThetaOverTwo;
        }
        return getRotationfromVector(deltaRotationVector);
    }

    private float[][] getRotationMatrixFromAxisAngle(float[] e, float theta){
        float mag = (float) Math.sqrt(e[0]*e[0] + e[1]*e[1] + e[2]*e[2]);
        float[] r_vec = new float[4];
        theta = theta/2.0f;
        r_vec[0] = e[0]/mag * (float) Math.sin(theta);
        r_vec[1] = e[1]/mag * (float) Math.sin(theta);
        r_vec[2] = e[2]/mag * (float) Math.sin(theta);
        r_vec[3] = (float) Math.cos(theta);
        return getRotationfromVector(r_vec);
    }

    private float[] convertRotationMat2RPY(float[][] R){
        float[] rpy = new float[3];
        rpy[2] = (float) Math.toDegrees((float) Math.atan2(R[1][0], R[0][0]));
        rpy[1] = (float) Math.toDegrees((float) Math.atan2(-R[2][0], Math.sqrt(R[2][1]*R[2][1] + R[2][2]*R[2][2])));
        rpy[0] = (float) Math.toDegrees((float) Math.atan2(R[2][1], R[2][2]));
        return rpy;
    }

    private float[][] mag_calibration(float[] N_L, float[] N_Grf, float[][] est_Ot){
        float[][] est_Ot_t = np_transpose(est_Ot);
        float[] est_N_L = matrix_dot_vector(est_Ot_t, N_Grf);
        float[] e = np_vector_cross(est_N_L, N_L);
        float angle = vector_angle(est_N_L, N_L);
        float[][] delta_R = getRotationMatrixFromAxisAngle(e, this.mg_cali_alpha*angle);
        return matrix_A_mul_matrix_B_T(est_Ot, delta_R); //tested in python and java
    }
    private float[][] gravity_calibration(float[] acc_L, float[] acc_Grf, float[][] est_Ot){
        float[][] est_Ot_t = np_transpose(est_Ot);
        float[] est_acc_L = matrix_dot_vector(est_Ot_t, acc_Grf);
        float[] e = np_vector_cross(est_acc_L, acc_L);
        float angle = vector_angle(est_acc_L, acc_L);
        float[][] delta_R = getRotationMatrixFromAxisAngle(e, this.acc_cali_alpha*angle);
        return matrix_A_mul_matrix_B_T(est_Ot, delta_R); //tested in python and java
    }


    class global_mag {
        float[] mag;
        int cnt;

        global_mag() {
            this.mag = new float[3];
            this.cnt = 0;
        }
        void init(float[] mag, int cnt) {
            this.mag[0] = mag[0];
            this.mag[1] = mag[1];
            this.mag[2] = mag[2];
            this.cnt = cnt;
        }
        void update(float[] mag) {
            this.mag[0] = (this.mag[0] * this.cnt + mag[0]) / (this.cnt + 1);
            this.mag[1] = (this.mag[1] * this.cnt + mag[1]) / (this.cnt + 1);
            this.mag[2] = (this.mag[2] * this.cnt + mag[2]) / (this.cnt + 1);
            this.cnt += 1;
        }
    }

    class orientation_mat{
        long timestamp;
        float[][] rotationMatrix;
        orientation_mat(long timestamp, float[][] rotationMatrix){
            this.timestamp = timestamp;
            this.rotationMatrix = rotationMatrix;
        }
        void updatetimestamp(long timestamp){
            this.timestamp = timestamp;
        }
        void update_rotationMatrix(float[][] rotationMatrix){
            this.rotationMatrix = rotationMatrix;
        }
        float[] get_rpy(){
            return convertRotationMat2RPY(this.rotationMatrix);
        }
    }

    class calibration{
        int mag_cnt;
        float[] mag;
        float[] gravity;
        calibration(){
            this.mag_cnt = 0;
            this.mag = new float[3];
            this.gravity = new float[3];
        }
        void update_mag(float[] mag){
            this.mag[0] += mag[0];
            this.mag[1] += mag[1];
            this.mag[2] += mag[2];
            this.mag_cnt += 1;
        }
        void update_gravity(float[] gravity){
            this.gravity[0] = gravity[0];
            this.gravity[1] = gravity[1];
            this.gravity[2] = gravity[2];
        }
        float[] get_mag(){
            if (this.mag_cnt == 0){
                return null;
            }
            return new float[]{this.mag[0] / this.mag_cnt, this.mag[1] / this.mag_cnt, this.mag[2] / this.mag_cnt};
        }
        float[] get_gravity(){
            return this.gravity;
        }
    }

    muse_plus(){
        this.initialized = false;
//        this.rotationMatrix = new float[9];
        this.acc = new float[3];
        this.gyro = new float[3];
        this.mag = new float[3];
        this.isMagAccessed = false;
        this.acc_calibrated = new float[3];
        this.gyro_calibrated = new float[3];
        this.rotate_theta = 0f;
        this.mcalibration = new calibration();

    }
    public void update_gravity(float[] gravity){
        this.mcalibration.update_gravity(gravity);
    }

    public void update_acc(float[] acc){
        if (!initialized){
            // if not initialized, put into calibration, otherwise, update global gravity
            if (oneD3Element_Norm(acc) > 9.5 && oneD3Element_Norm(acc) < 10.5){
                this.mcalibration.update_gravity(acc);
            }
        }
        else{
            // update global acceleration
            this.acc = acc;
        }
    }
    // update magnetic field, if not initialized, put into calibration,otherwise, update global magnetic field
    public void update_mag(float[] mag, long timestamp){
        if (!initialized){
            // if not initialized, put into calibration
            if (oneD3Element_Norm(mag) > lowermglimit && oneD3Element_Norm(mag) < uppermglimit){
                this.mcalibration.update_mag(mag);
            }
            if (this.mcalibration.mag_cnt >= 3){
                // if calibration is done, initialize the rotation matrix
                // if satisfy the condition, initialize the rotation matrix
                cur_orientation = new orientation_mat(timestamp,getOrientationfromGravityandMag(this.mcalibration.get_gravity(), this.mcalibration.get_mag()));
                this.mGlobalMag = new global_mag();
                this.mGlobalMag.init(matrix_dot_vector(cur_orientation.rotationMatrix, this.mcalibration.get_mag()), 1000);
                this.mGlobalGravity = matrix_dot_vector(cur_orientation.rotationMatrix, this.mcalibration.get_gravity());
                this.initialized = true;
            }
        }
        else{
            // update most recent lrf magnetic field measurements
            this.mag = mag;
            this.isMagAccessed = false;
        }
    }
    public void update_gyro(float[] gyro, long timestamp){
        this.gyro = gyro;
        if (this.initialized){
            // init: requires at least 3 mag data within range

            // update rotation matrix
            // update acc_calibrated
            // update gyro_calibrated
            //under certain situations, will compensate the orientation matrix
            // calibrate, deltaR, update orientation
            float[][] delta_R = deltaRfromGyro(timestamp, gyro, this.gyro_timestamp);
            float[][] orient_old = matrix_A_mul_matrix_B(cur_orientation.rotationMatrix, delta_R);

            // calculate adjusted acc and gyro
            this.gyro_calibrated = matrix_dot_vector(cur_orientation.rotationMatrix, this.gyro);
            this.acc_calibrated = matrix_dot_vector(cur_orientation.rotationMatrix, this.acc);
            long mdTinNS = timestamp - this.gyro_timestamp;
            rotate_theta += Math.toDegrees(this.gyro_calibrated[2] * mdTinNS * NS2S);

            // if magnetic field magnitude within range
            boolean isMagCalibrated = false;
//            if (oneD3Element_Norm(this.mag) > lowermglimit && oneD3Element_Norm(this.mag) < uppermglimit){
            if (!isMagAccessed && oneD3Element_Norm(this.mag) < 90 && oneD3Element_Norm(this.mag) > 20){
                cur_orientation.update_rotationMatrix(mag_calibration(this.mag, this.mGlobalMag.mag, orient_old));
                isMagCalibrated = true;
                isMagAccessed = true;
            }
            // rangeg = 0.3
            if (oneD3Element_Norm(this.acc) > 9.5 && oneD3Element_Norm(this.acc) < 10.1){
                cur_orientation.update_rotationMatrix(gravity_calibration(this.acc, this.mGlobalGravity, orient_old));
                if (oneD3Element_Norm(this.mag) < 90 && oneD3Element_Norm(this.mag) > 20){
                    this.mGlobalMag.update(matrix_dot_vector(cur_orientation.rotationMatrix, this.mag));
                }
            }
//            Log.d("muse_plus", "mag: " + Arrays.toString(cur_orientation.get_rpy()));
        }
        this.gyro_timestamp = timestamp;
    }

    public void restart_calibration(){
        this.initialized = false;
        this.mGlobalMag = new global_mag();
    }

    public float[] get_oriented_acc(){
        return this.acc_calibrated;
    }
    public float[] get_oriented_gyro(){
        return this.gyro_calibrated;
    }

    public float[] get_EulerAngles(){
        if (!initialized){
            return null;
        }
        return cur_orientation.get_rpy();
    }

    public float get_rotate_theta(){
        // return rotate theta and set it to 0
        float tmp = this.rotate_theta;
        this.rotate_theta = 0f;
        return tmp;
    }

}
