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

############################################################################
########################## Initialize Model ################################
############################################################################
model_url = "https://tfhub.dev/tensorflow/efficientnet/lite0/feature-vector/2"
IMAGE_SHAPE = (224, 224)

layer = hub.KerasLayer('C:\\Users\\afifi\\Downloads\\server\\')
model = tf.keras.Sequential([layer])

#batch1 = ['test1.jpg', 'test3.jpg']
#batch2 = ['test2.jpg', 'test4.jpg']
batch1 = []
batch2 = []
count = 0
image_name = ""

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

def extract_frames_and_save(input_video_path):
    cap = cv2.VideoCapture(input_video_path)

    if not cap.isOpened():
        print("Error: Could not open video file.")
        return

    frame_count = 0
    filenames = []
    save_path = 'C:\\Users\\afifi\\Downloads\\server\\'  

    while True:
        ret, frame = cap.read()

        if not ret:
            break

        frame_count += 1

        # Save every 10th frame
        if frame_count % 10 == 0:
            _, jpeg_data = cv2.imencode('.jpg', frame)
            jpeg_array = np.array(jpeg_data).tobytes()

            global count
            if count == 1:
              output_filename = f"frame2_{frame_count // 10}.jpg"
              output_path = os.path.join(save_path, output_filename)
            
            elif count == 0:
              output_filename = f"frame_{frame_count // 10}.jpg"
              output_path = os.path.join(save_path, output_filename)

            with open(output_path, 'wb') as f:
                f.write(jpeg_array)

            filenames.append(output_filename)

    cap.release()
    count = count + 1
    return filenames

def extract(file):
  file = Image.open(file).convert('L').resize(IMAGE_SHAPE)
  file = np.stack((file,)*3, axis=-1)

  file = np.array(file)/255.0
  embedding = model.predict(file[np.newaxis, ...])

  vgg16_feature_np = np.array(embedding)
  flattended_feature = vgg16_feature_np.flatten()

  return flattended_feature


def extract_features(batch1, batch2):
  batch1_vals = []
  batch2_vals = []
  for i in range (len(batch1)):
      try:
        batch1_vals.append(extract(batch1[i]))
        batch2_vals.append(extract(batch2[i]))
      except IndexError:
         pass
  
  return batch1_vals, batch2_vals

def most_similar(batch1, batch2):
  metric = 'cosine'
  batch1_vals, batch2_vals = extract_features(batch1, batch2)
  best_match = [0,0]
  best_dc = 1
  best_i = 0
  for i in range(len(batch1_vals)):
    try:
      dc = distance.cdist([batch1_vals[i]], [batch2_vals[i]], metric)[0][0]
    except IndexError:
       pass
    if dc < best_dc:
      best_dc = dc
      best_i = i
      best_match[0] = batch1_vals[i]   
      best_match[1] = batch2_vals[i]

  return batch1[i]



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
  save_path = 'C:\\Users\\afifi\\Downloads\\server\\'  

  video_path = os.path.join(save_path, video_filename)

  if os.path.exists(video_path):
      os.remove(video_path)

  video_file.save(video_path)
  #video_file.save(os.path.join(save_path, video_filename))
  print(f"Video saved to: {os.path.join(save_path, video_filename)}")

  input_video_path = f'C:/Users/afifi/Downloads/server/{video_filename}'
  print(input_video_path)
  global batch1
  global batch2    
  global image_name

  if count == 1:
    batch2 = extract_frames_and_save(input_video_path)
    image_name = most_similar(batch1, batch2)
    image_base64 = image_to_base64(image_name)
    data = {'image_encoded': image_base64}
    return jsonify(data)
  
  elif count > 1:
    image_base64 = image_to_base64(image_name)
    data = {'image_encoded': image_base64}
    return jsonify(data)
  
  elif count == 0: 
    batch1 = extract_frames_and_save(input_video_path)
    data = {'image_encoded': "check back soon!"}
    return jsonify(data)
  
  data = {'image_encoded': "too many calls!"}
  return jsonify(data)


@app.route('/check', methods=['GET'])
def check():
  image_base64 = image_to_base64(image_name)
  data = {'image_encoded': image_base64}
  return jsonify(data)

############################################################################
################################ Run Server ################################
############################################################################
if __name__ == '__main__':
    app.run(debug=True)
