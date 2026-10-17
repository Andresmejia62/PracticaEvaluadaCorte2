module com.appregistros.practicaevaluadacorte2 {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.appregistros.practicaevaluadacorte2 to javafx.fxml;
    exports com.appregistros.practicaevaluadacorte2;
}