package com.github.alphardpaarthurnax.bohcalculator.controller;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RichDescriptionRenderer {
    private static final Pattern MARKUP = Pattern.compile("<b>(.*?)</b>|<i>(.*?)</i>|<sprite name=(.*?)>");

    private RichDescriptionRenderer() {
    }

    static List<Node> render(String description) {
        List<Node> nodes = new ArrayList<>();
        if (description == null || description.isEmpty()) {
            return nodes;
        }

        Matcher matcher = MARKUP.matcher(description);
        int lastEnd = 0;
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                nodes.add(new Text(description.substring(lastEnd, matcher.start())));
            }
            nodes.add(markupNode(matcher));
            lastEnd = matcher.end();
        }
        if (lastEnd < description.length()) {
            nodes.add(new Text(description.substring(lastEnd)));
        }
        return nodes;
    }

    private static Node markupNode(Matcher matcher) {
        if (matcher.group(1) != null) {
            Text text = new Text(matcher.group(1));
            text.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
            return text;
        }
        if (matcher.group(2) != null) {
            Text text = new Text(matcher.group(2));
            text.setStyle("-fx-fill: #c41d7f; -fx-font-style: italic;");
            return text;
        }
        ImageView image = new ImageView();
        image.setFitWidth(20);
        image.setFitHeight(20);
        image.setPreserveRatio(true);
        image.setTranslateY(3);
        try (InputStream input = RichDescriptionRenderer.class.getResourceAsStream(
                "/assets/images/aspects/" + matcher.group(3) + ".png")) {
            if (input != null) {
                image.setImage(new Image(input));
            }
        } catch (java.io.IOException ignored) {
        }
        return image;
    }
}
