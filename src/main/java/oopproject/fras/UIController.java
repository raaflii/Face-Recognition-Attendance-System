package oopproject.fras;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UIController {
    @FXML
    private TextField namaField;
    @FXML
    private TextField nimField;
    @FXML
    private TextField prodiField;
    @FXML
    private TextField fakultasField;
    @FXML
    private Button registerBtn;
    @FXML
    private Button identifyBtn;
    @FXML
    private Button onOffBtn;
    @FXML
    private Button attendanceBtn;
    @FXML
    private ImageView frameCamera;
    @FXML
    private ListView<String> logList;
    @FXML
    private ListView<String> userList;

    private FaceRecognizer faceRecognizer;
    private Database db;
    private VideoCapture camera;
    private Thread cameraThread;
    private Mat frame;
    private boolean cameraNyala = false;
    private boolean identifikasiNyala = false;
    private FaceDetection faceDetection;

    public void initialize() {
        faceRecognizer = new FaceRecognizer("src/main/resources/oopproject/fras/haarcascade/haarcascade_frontalface_default.xml");
        db = new Database();
        faceDetection = new FaceDetection("src/main/resources/oopproject/fras/haarcascade/haarcascade_frontalface_default.xml");

        registerBtn.setOnAction(actionEvent -> registerUser());
        identifyBtn.setOnAction(actionEvent -> toggleFaceDetection());
        onOffBtn.setOnAction(actionEvent -> toggleCamera());
        attendanceBtn.setOnAction(actionEvent -> tampilkanUser());
    }

    private void toggleCamera() {
        Image on = new Image(getClass().getResource("/oopproject/fras/img/camera4.png").toExternalForm());
        Image off = new Image(getClass().getResource("/oopproject/fras/img/stop2.png").toExternalForm());

        ImageView onImageView = new ImageView(on);
        onImageView.setFitWidth(64);
        onImageView.setFitHeight(56);

        ImageView offImageView = new ImageView(off);
        offImageView.setFitWidth(57);
        offImageView.setFitHeight(54);

        if (cameraNyala) {
            stopCamera();
            onOffBtn.setGraphic(onImageView);
            tampilkanLog("Kamera Mati");
        } else {
            startCamera();
            onOffBtn.setGraphic(offImageView);
            tampilkanLog("Kamera Hidup");
        }
    }

    private boolean cekDataDiDB() {
        boolean exists = false;

        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/faceRecogAttendanceList", "root", "")) {
            String query = "SELECT COUNT(*) AS total FROM users";
            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                exists = rs.getInt("total") > 0; // Jika total > 0, data ada
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return exists;
    }


    private void toggleFaceDetection() {
        if (cameraNyala) {
            identifikasiNyala = !identifikasiNyala;
            if (identifikasiNyala) {
                if (cekDataDiDB()) {
                    faceRecognizer.trainRecognizerFromDatabase();
                } else {
                    tampilkanLog("Tidak Ada Data Apapun Di Database!! Silakan Registrasi");
                }
            }
        } else {
            tampilkanLog("Harap Hidupkan Kamera Sebelum Deteksi Wajah");
        }
    }

    private void startCamera() {
        camera = new VideoCapture();
        if (!camera.open(0)) {
            tampilkanLog("Kamera Tidak Terbuka");
            return;
        }

        cameraNyala = true;

        cameraThread = new Thread(() -> {
            while (cameraNyala) {
                captureFrame();
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    tampilkanLog("Thread Kamera dihentikan");
                }
            }
        });

        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private void stopCamera() {
        cameraNyala = false;

        if (cameraThread != null) {
            try {
                cameraThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (camera.isOpened()) {
            camera.release();
        }

        Platform.runLater(() -> frameCamera.setImage(null));
    }

    private void captureFrame() {
        frame = new Mat();
        if (camera.read(frame) && !frame.empty()) {
            if (identifikasiNyala) {
                detectAndDrawFaces(frame);
            }
            Image img = matToImage(frame);

            Platform.runLater(() -> frameCamera.setImage(img));
        }
    }

    private void detectAndDrawFaces(Mat frame) {
        Mat grayFrame = new Mat();
        org.opencv.imgproc.Imgproc.cvtColor(frame, grayFrame, org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY);

        Rect[] faces = faceDetection.detectFaces(grayFrame);
        frame = faceDetection.drawRectangles(frame, faces);

        if (identifikasiNyala) {
            // Lakukan identifikasi wajah setelah mendeteksi wajah
            String recognizedLabel = faceRecognizer.recognizeFace(frame);
            drawFaceBoxAndLabel(frame, recognizedLabel != null ? recognizedLabel : "Unknown");
        }
    }

    private void drawFaceBoxAndLabel(Mat frame, String label) {
        Rect[] faces = faceDetection.detectFaces(frame);
        for (Rect face : faces) {
            Imgproc.rectangle(frame, face.tl(), face.br(), new Scalar(0, 255, 0), 2);
            Imgproc.putText(frame, label, new org.opencv.core.Point(face.x, face.y - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, new Scalar(0, 255, 0), 2);
        }
    }

    private void registerUser() {
        String nama = namaField.getText();
        String nim = nimField.getText();
        String prodi = prodiField.getText();
        String fakultas = fakultasField.getText();

        if (nama.isEmpty() || nim.isEmpty() || prodi.isEmpty() || fakultas.isEmpty()) {
            tampilkanLog("Harap Isi Semua Form Register");
            return;
        }

        if (!cameraNyala) {
            tampilkanLog("Harap Hidupkan Kamera Sebelum Registrasi");
            return;
        }

        // Ambil frame langsung dari kamera
        byte[][] faceImages = faceRecognizer.captureFaceImages(camera);
        if (faceImages != null) {
            db.saveUserData(nama, nim, prodi, fakultas, faceImages);
            faceRecognizer.trainRecognizerFromDatabase();
            namaField.clear();
            nimField.clear();
            prodiField.clear();
            fakultasField.clear();
        } else {
            tampilkanLog("Gagal Menangkap Gambar Wajah");
        }
    }

    private Image matToImage(Mat mat) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mat, buffer);
        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    private void tampilkanLog(String msg) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String tanggalWaktu = LocalDateTime.now().format(formatter);

        String log = tanggalWaktu + " - " + msg;

        Platform.runLater(() -> {
            logList.getItems().add(log);
            logList.scrollTo(logList.getItems().size() - 1);
        });
    }

    private void tampilkanUser() {
        if (!cameraNyala || !identifikasiNyala) {
            tampilkanLog("Harap Hidupkan Kamera dan Aktifkan Identifikasi Wajah Sebelum Mengambil Kehadiran");
            return;
        }

        String recognizedName = faceRecognizer.recognizeFace(frame); // Hasil identifikasi nama pengguna
        if (recognizedName == null || recognizedName.equals("Unknown")) {
            tampilkanLog("Wajah Tidak Dikenali! Harap Registrasi Terlebih Dahulu");
            return;
        }

        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/faceRecogAttendanceList", "root", "")) {
            String query = "SELECT nama, nim, prodi, fakultas FROM users WHERE nama = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, recognizedName);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Simpan data dari ResultSet ke variabel lokal
                String nama = rs.getString("nama");
                String nim = rs.getString("nim");
                String prodi = rs.getString("prodi");
                String fakultas = rs.getString("fakultas");

                // Gunakan Platform.runLater untuk memperbarui UI
                Platform.runLater(() -> {
                    userList.getItems().clear(); // Hapus daftar sebelumnya
                    userList.getItems().add("Nama: " + nama);
                    userList.getItems().add("NIM: " + nim);
                    userList.getItems().add("Prodi: " + prodi);
                    userList.getItems().add("Fakultas: " + fakultas);
                    userList.getItems().add("Status: Hadir");
                });
                tampilkanLog(recognizedName + " Hadir. Data Kehadiran Ditampilkan!");
            } else {
                tampilkanLog("Data Tidak Ditemukan di Database. Harap Registrasi Terlebih Dahulu");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            tampilkanLog("Terjadi Kesalahan Saat Mengakses Database");
        }
    }

}