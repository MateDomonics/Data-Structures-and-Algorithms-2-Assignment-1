module com.matedomonics.assignment1 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.matedomonics.assignment1 to javafx.fxml;
    exports com.matedomonics.assignment1;
}