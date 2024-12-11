package oopproject.fras;

import org.opencv.core.*;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FaceRecognizer {
    private VideoCapture camera;
    private CascadeClassifier faceDetector;
    private LBPHFaceRecognizer recognizer;
    private boolean cameraRunning;
    private boolean isTrained = false;



    public FaceRecognizer(String haarCascadePath) {
        this.camera = new VideoCapture(0);
        this.faceDetector = new CascadeClassifier(haarCascadePath);
        this.recognizer = LBPHFaceRecognizer.create();
        this.cameraRunning = false;
    }

    // Metode untuk mengambil gambar wajah dan mendeteksi wajah
    public byte[][] captureFaceImages(VideoCapture camera) {
        if (!camera.isOpened()) {
            System.out.println("Kamera tidak aktif");
            return null;
        }

        ArrayList<byte[]> faceImages = new ArrayList<>();
        int captureCount = 10;

        for (int i = 0; i < captureCount; i++) {
            Mat frame = new Mat();
            if (!camera.read(frame)) {
                System.out.println("Gagal membaca frame");
                continue;
            }

            // Deteksi wajah dalam frame
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            MatOfRect faces = new MatOfRect();
            faceDetector.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(30, 30), new Size());

            for (Rect face : faces.toArray()) {
                Mat faceROI = frame.submat(face);
                Imgproc.cvtColor(faceROI, faceROI, Imgproc.COLOR_BGR2GRAY);
                byte[] faceImage = matToByteArray(faceROI);
                faceImages.add(faceImage);
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return faceImages.toArray(new byte[0][]);
    }

    // Konversi gambar dari Mat ke byte array untuk penyimpanan
    private byte[] matToByteArray(Mat mat) {
        MatOfByte byteMat = new MatOfByte();
        Imgcodecs.imencode(".jpg", mat, byteMat);
        return byteMat.toArray();
    }

    // Training model menggunakan gambar dari database
    public void trainRecognizerFromDatabase() {
        List<Mat> images = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/faceRecogAttendanceList", "root", "")) {
            String query = "SELECT id, face_image_1, face_image_2, face_image_3, face_image_4, face_image_5, face_image_6, face_image_7, face_image_8, face_image_9, face_image_10 FROM users";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");

                for (int i = 1; i <= 10; i++) {
                    byte[] imgBytes = rs.getBytes("face_image_" + i);
                    if (imgBytes == null || imgBytes.length == 0) {
                        System.out.println("Gambar kosong untuk ID: " + id + ", kolom: face_image_" + i);
                        continue;
                    }

                    Mat img = Imgcodecs.imdecode(new MatOfByte(imgBytes), Imgcodecs.IMREAD_GRAYSCALE);
                    if (img.empty()) {
                        System.out.println("Gambar korup untuk ID: " + id + ", kolom: face_image_" + i);
                        continue;
                    }

                    Imgproc.resize(img, img, new Size(100, 100));
                    Core.normalize(img, img, 0, 255, Core.NORM_MINMAX);
                    images.add(img);
                    labels.add(id);
                }
            }

            if (!images.isEmpty()) {
                recognizer.train(images, new MatOfInt(labels.stream().mapToInt(i -> i).toArray()));
                isTrained = true;
                System.out.println("Model berhasil dilatih dan disimpan.");
            } else {
                System.out.println("Tidak ada data yang cukup untuk pelatihan.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    // Proses identifikasi wajah berdasarkan frame kamera
    public String recognizeFace(Mat frame) {
        if (!isTrained) {
            return "Unknown";
        }

        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(gray, faces, 1.1, 3, 0, new Size(30, 30), new Size());

        for (Rect face : faces.toArray()) {
            Mat faceROI = gray.submat(face);
            int[] predictedLabel = new int[1];
            double[] confidence = new double[1];

            recognizer.predict(faceROI, predictedLabel, confidence);
            if (predictedLabel[0] != -1 && confidence[0] < 70) {
                return getNamaByUserID(predictedLabel[0]);
            }
        }

        return "Unknown";
    }

    private String getNamaByUserID(int userId) {
        String userName = "Unknown";

        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/faceRecogAttendanceList", "root", "")) {
            String query = "SELECT nama FROM users WHERE id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                userName = rs.getString("nama");  // Ambil nama berdasarkan ID
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return userName;
    }
}
