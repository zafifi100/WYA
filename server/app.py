from flask import Flask, jsonify, request
import tensorflow as tf
import tensorflow_hub as hub
from scipy.spatial import distance
from PIL import Image
import numpy as np
import os
from werkzeug.utils import secure_filename
import cv2 as cv
import numpy as np
import base64
import json

############################################################################
########################## Initialize Model ################################
############################################################################
model_url = "https://tfhub.dev/tensorflow/efficientnet/lite0/feature-vector/2"
IMAGE_SHAPE = (224, 224)

layer = hub.KerasLayer('C:\\Users\\apmoo\\ECE454\\WYA\\server\\')
model = tf.keras.Sequential([layer])

#batch1 = ['test1.jpg', 'test3.jpg']
#batch2 = ['test2.jpg', 'test4.jpg']
images1 = []
images2 = []
count = 0
image_name = ""

yaws1 = []
yaws2 = []
user1 = ""
user2 = ""
dict1 = {}
dict2 = {}
most_similar_key = -1
yaw_user1 = -1
yaw_user2 = -1


app = Flask(__name__)

############################################################################
################################ Test API ##################################
############################################################################
@app.route('/test', methods=['GET'])
def example_api():
    data = {'message': 'Hello from the server!'}
    return jsonify(data)


############################################################################
################################ Actual Model ##############################
############################################################################
import cv2
import numpy as np

# def extract_frames_and_save(input_video_path):
#     cap = cv2.VideoCapture(input_video_path)

#     if not cap.isOpened():
#         print("Error: Could not open video file.")
#         return

#     frame_count = 0
#     filenames = []
#     save_path = 'C:\\Users\\apmoo\\ECE454\\WYA\\server\\'  

#     while True:
#         ret, frame = cap.read()

#         if not ret:
#             break

#         frame_count += 1

#         # Save every 10th frame
#         if frame_count % 10 == 0:
#             _, jpeg_data = cv2.imencode('.jpg', frame)
#             jpeg_array = np.array(jpeg_data).tobytes()

#             global count
#             if count == 1:
#               output_filename = f"frame2_{frame_count // 10}.jpg"
#               output_path = os.path.join(save_path, output_filename)
            
#             elif count == 0:
#               output_filename = f"frame_{frame_count // 10}.jpg"
#               output_path = os.path.join(save_path, output_filename)

#             with open(output_path, 'wb') as f:
#                 f.write(jpeg_array)

#             filenames.append(output_filename)

#     cap.release()
#     count = count + 1
#     return filenames


# should now only save 18 frames from the video
def extract_frames_and_save(input_video_path):
    cap = cv2.VideoCapture(input_video_path)

    if not cap.isOpened():
        print("Error: Could not open video file.")
        return

    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    interval = max(1, total_frames // 18)  # Calculate interval, ensure it's at least 1

    frame_count = 0
    saved_frame_count = 0
    filenames = []
    save_path = 'C:\\Users\\apmoo\\ECE454\\WYA\\server\\'  

    while True:
        ret, frame = cap.read()

        if not ret:
            break

        frame_count += 1

        # Save a frame only at the calculated intervals
        if frame_count % interval == 0:
            _, jpeg_data = cv2.imencode('.jpg', frame)
            jpeg_array = np.array(jpeg_data).tobytes()

            global count
            if count == 1:
                output_filename = f"frame2_{saved_frame_count + 1}.jpg"
                output_path = os.path.join(save_path, output_filename)
            elif count == 0:
                output_filename = f"frame_{saved_frame_count + 1}.jpg"
                output_path = os.path.join(save_path, output_filename)

            with open(output_path, 'wb') as f:
                f.write(jpeg_array)

            filenames.append(output_filename)
            saved_frame_count += 1

            # Stop if 18 frames are saved
            if saved_frame_count >= 18:
                break

    cap.release()
    count = count + 1
    return filenames

def sift_similarity(img1, img2):
    # Initialize SIFT detector
    sift = cv2.SIFT_create()

    # Find the keypoints and descriptors with SIFT
    kp1, des1 = sift.detectAndCompute(img1, None)
    kp2, des2 = sift.detectAndCompute(img2, None)

    # BFMatcher with default params
    bf = cv2.BFMatcher()
    matches = bf.knnMatch(des1, des2, k=2)

    # Apply ratio test
    good_matches = []
    for m, n in matches:
        if m.distance < 0.75 * n.distance:
            good_matches.append([m])

    return len(good_matches)

def most_similar_image(dict1, dict2):
    most_similar_key = None
    highest_similarity = 0

    for key in dict1:
        if key in dict2:
            img1 = cv2.imread(dict1[key], 0)
            img2 = cv2.imread(dict2[key], 0)

            similarity = sift_similarity(img1, img2)
            
            if similarity > highest_similarity:
                highest_similarity = similarity
                most_similar_key = key

    return most_similar_key

def find_zoom_factor(img1, img2):
    # Initialize SIFT detector
    sift = cv2.SIFT_create()

    # Find the keypoints and descriptors with SIFT
    kp1, des1 = sift.detectAndCompute(img1, None)
    kp2, des2 = sift.detectAndCompute(img2, None)

    # Match features using KNN
    bf = cv2.BFMatcher()
    matches = bf.knnMatch(des1, des2, k=2)

    # Apply ratio test to find good matches
    good_matches = []
    for m, n in matches:
        if m.distance < 0.75 * n.distance:
            good_matches.append(m)

    # Extract location of good matches
    points1 = np.float32([kp1[m.queryIdx].pt for m in good_matches])
    points2 = np.float32([kp2[m.trainIdx].pt for m in good_matches])

    # Find homography
    H, _ = cv2.findHomography(points1, points2, cv2.RANSAC, 5.0)

    # Calculate zoom factor (simple approximation)
    zoom_factor = np.mean([H[0, 0], H[1, 1]])

    return zoom_factor



# def extract(file):
#   file = Image.open(file).convert('L').resize(IMAGE_SHAPE)
#   file = np.stack((file,)*3, axis=-1)

#   file = np.array(file)/255.0
#   embedding = model.predict(file[np.newaxis, ...])

#   vgg16_feature_np = np.array(embedding)
#   flattended_feature = vgg16_feature_np.flatten()

#   return flattended_feature

# def extract_features(batch1, batch2):
#   batch1_vals = []
#   batch2_vals = []
#   for i in range (len(batch1)):
#       try:
#         batch1_vals.append(extract(batch1[i]))
#         batch2_vals.append(extract(batch2[i]))
#       except IndexError:
#          pass
  
#   return batch1_vals, batch2_vals

# def most_similar(batch1, batch2):
#   metric = 'cosine'
#   batch1_vals, batch2_vals = extract_features(batch1, batch2)
#   best_match = [0,0]
#   best_dc = 1
#   best_i = 0
#   for i in range(len(batch1_vals)):
#     try:
#       dc = distance.cdist([batch1_vals[i]], [batch2_vals[i]], metric)[0][0]
#     except IndexError:
#        pass
#     if dc < best_dc:
#       best_dc = dc
#       best_i = i
#       best_match[0] = batch1_vals[i]   
#       best_match[1] = batch2_vals[i]

#   return batch1[i]



############################################################################################



def image_to_base64(image_path):
    with open(image_path, "rb") as image_file:
        encoded_image = base64.b64encode(image_file.read()).decode('utf-8')
    return encoded_image


@app.route('/predict', methods=['POST'])
def predict():
  print("Incoming request data:", request.files)
  video_file = request.files['video']
  video_filename = video_file.filename

  # declare global
  global user1
  global user2
  global yaws1
  global yaws2
  global yaw_user1
  global yaw_user2

  # read in yaws from arrayList
  integers_json = request.form['integerList']
  try:
     integers_list = json.loads(integers_json)
     print(integers_list)
  except json.JSONDecodeError:
     return jsonify({'error': 'Invalid JSON for integers'}), 400
  
  # read in usernames and assign yaws
  if count == 0:
    user1 = request.form.get('username', '')
    yaws1 = integers_list

  elif count == 1:
    user2 = request.form.get('username', '')
    yaws2 = integers_list


  save_path = 'C:\\Users\\apmoo\\ECE454\\WYA\\server\\'  
  video_path = os.path.join(save_path, video_filename)

  if os.path.exists(video_path):
      os.remove(video_path)

  video_file.save(video_path)
  #video_file.save(os.path.join(save_path, video_filename))
  print(f"Video saved to: {os.path.join(save_path, video_filename)}")

  input_video_path = f'C:/Users/apmoo/ECE454/WYA/server/{video_filename}'
  print(input_video_path)
  global images1
  global images2    
  global image_name
  global dict1
  global dict2
  global most_similar_key

  # first video to server
  if count == 0: 
    images1 = extract_frames_and_save(input_video_path)
    dict1 = dict(zip(yaws1, images1))

    # data = {'image_encoded': "check back soon!"}
    data = {'status': "check back soon!"}
    return jsonify(data)
  
  # second video to server
  elif count == 1:
    images2 = extract_frames_and_save(input_video_path)
    dict2 = dict(zip(yaws2, images2))

    # call SIFT algorithm
    most_similar_key = most_similar_image(dict1, dict2)
    img1 = cv2.imread(dict1[most_similar_key], 0)
    img2 = cv2.imread(dict2[most_similar_key], 0)

    # determine zoom factor
    zoom_factor = find_zoom_factor(img1, img2)
    if zoom_factor > 1:
       yaw_user1 = most_similar_key
       # sping user2 around, math to keep yaw [-180,180]
       if most_similar_key < 0:
          yaw_user2 = most_similar_key + 180
       elif most_similar_key > 0:
          yaw_user2 = most_similar_key - 180

    elif zoom_factor == 1:
       yaw_user1 = most_similar_key
       yaw_user2 = most_similar_key

    elif zoom_factor < 1 and zoom_factor > 0:
       # spin user1 around, math to keep yaw [-180,180]
       if most_similar_key < 0:
          yaw_user1 = most_similar_key + 180
       elif most_similar_key > 0:
          yaw_user1 = most_similar_key - 180

       yaw_user2 = most_similar_key

    elif zoom_factor < 0 or None:
       yaw_user1 = most_similar_key
       yaw_user2 = most_similar_key

    username = request.args.get('username', None)
    yaw = 0
    if username == user1:
      return jsonify({'yaw' : yaw_user1, 'status' : 'done'})
    elif username == user2:
      return jsonify({'yaw' : yaw_user2, 'status' : 'done'})
    return jsonify({'yaw' : yaw, 'status' : 'done'})

    # image_name = most_similar(images1, images2)
    # image_base64 = image_to_base64(image_name)
    # data = {'image_encoded': image_base64}
    # return jsonify(data)
  

  # both videos sent to server, returns most similar image if available
  elif count > 1:
    username = request.args.get('username', None)
    yaw = 0
    if username == user1:
      return jsonify({'yaw' : yaw_user1, 'status' : 'done'})
    elif username == user2:
      return jsonify({'yaw' : yaw_user2, 'status' : 'done'})
    return jsonify({'yaw' : yaw, 'status' : 'done'})
  
    # image_base64 = image_to_base64(image_name)
    # data = {'image_encoded': image_base64}
    # return jsonify(data)
  

  # data = {'image_encoded': "too many calls!"}
  data = {'status': "too many calls!"}
  return jsonify(data)


@app.route('/check', methods=['GET'])
def check():
  username = request.args.get('username', None)
  yaw = 0
  if username == user1:
    return jsonify({'yaw' : yaw_user1, 'status' : 'done'})
    #  yaw = yaw_user1
  elif username == user2:
    return jsonify({'yaw' : yaw_user2, 'status' : 'done'})
    #  yaw = yaw_user2
  return jsonify({'yaw' : yaw, 'status' : 'done'})

  # image_base64 = image_to_base64(image_name)
  # data = {'image_encoded': image_base64}
  # return jsonify(data)

############################################################################
################################ Run Server ################################
############################################################################
if __name__ == '__main__':
    app.run(debug=True)
