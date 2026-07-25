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

    opens com.github.alphardpaarthurnax.bohcalculator to javafx.fxml;
    exports com.github.alphardpaarthurnax.bohcalculator;
}