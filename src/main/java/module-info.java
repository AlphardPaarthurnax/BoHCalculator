module com.github.alphardpaarthurnax.bohcalculator {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires org.jsoup;

    opens com.github.alphardpaarthurnax.bohcalculator to javafx.fxml;
    opens com.github.alphardpaarthurnax.bohcalculator.controller to javafx.fxml;
    opens com.github.alphardpaarthurnax.bohcalculator.model to com.fasterxml.jackson.databind;
    exports com.github.alphardpaarthurnax.bohcalculator;
    exports com.github.alphardpaarthurnax.bohcalculator.model;
}