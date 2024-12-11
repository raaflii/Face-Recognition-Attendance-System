module oopproject.fras {
    requires javafx.controls;
    requires javafx.fxml;
    requires opencv;
    requires java.sql;


    opens oopproject.fras to javafx.fxml;
    exports oopproject.fras;
}