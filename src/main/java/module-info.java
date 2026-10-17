module com.appregistros.practicaevaluadacorte2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.postgresql.jdbc;

    opens com.appregistros.practicaevaluadacorte2 to javafx.fxml;
    opens com.appregistros.practicaevaluadacorte2.controller to javafx.fxml;
    exports com.appregistros.practicaevaluadacorte2;
    exports com.appregistros.practicaevaluadacorte2.Database;
    exports com.appregistros.practicaevaluadacorte2.controller;
    exports com.appregistros.practicaevaluadacorte2.model;
}