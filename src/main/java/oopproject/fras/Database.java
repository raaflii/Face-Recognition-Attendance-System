package oopproject.fras;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Database {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/faceRecogAttendanceList";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private Connection connection;

    public Database() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveUserData(String nama, String nim, String prodi, String fakultas, byte[][] faceImages) {
        // Pastikan hanya 14 parameter, tanpa `id` karena itu auto_increment
        String insertQuery = "INSERT INTO users (nama, nim, prodi, fakultas, face_image_1, face_image_2, face_image_3, face_image_4, face_image_5, face_image_6, face_image_7, face_image_8, face_image_9, face_image_10) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
            // Menyiapkan parameter pertama hingga keempat untuk `nama`, `nim`, `prodi`, `fakultas`
            stmt.setString(1, nama);
            stmt.setString(2, nim);
            stmt.setString(3, prodi);
            stmt.setString(4, fakultas);

            // Memastikan hanya 10 gambar yang dimasukkan
            for (int i = 0; i < 10; i++) {
                if (i < faceImages.length) {
                    stmt.setBytes(i + 5, faceImages[i]); // Mulai dari parameter ke-5 untuk face_image_1
                } else {
                    stmt.setNull(i + 5, java.sql.Types.BLOB); // Menangani jika gambar kurang dari 10
                }
            }

            // Eksekusi query untuk menyimpan data
            stmt.executeUpdate();
            System.out.println("Data berhasil disimpan");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
