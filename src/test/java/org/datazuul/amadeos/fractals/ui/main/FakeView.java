package org.datazuul.amadeos.fractals.ui.main;

import org.datazuul.amadeos.fractals.domain.ColorSettings;
import org.datazuul.amadeos.fractals.domain.FractalSettings;
import org.datazuul.amadeos.fractals.repository.FractalsPreferences.Bar;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

/** Records what the presenter shows and answers its questions from queues. */
final class FakeView implements MainView {
    Dimension canvas = new Dimension(160, 120);
    String title;
    FractalSettings fractal;
    ColorSettings colors;
    Location location;
    boolean autoIterations;
    BufferedImage picture;
    final List<BufferedImage> pictures = new ArrayList<>();
    String status;
    String pointer;
    String zoom;
    OptionalDouble progress;
    boolean progressVisible;
    Commands commands = Commands.NONE;
    boolean cycling;
    boolean disposed;
    BufferedImage clipboardImage;
    String clipboardText;
    final Map<Bar, Boolean> bars = new EnumMap<>(Bar.class);
    final List<String> errors = new ArrayList<>();
    final List<String> infos = new ArrayList<>();
    final Deque<Optional<Path>> openAnswers = new ArrayDeque<>();
    final Deque<Optional<Path>> parameterSaveAnswers = new ArrayDeque<>();
    final Deque<Optional<SaveTarget>> imageSaveAnswers = new ArrayDeque<>();
    final Deque<Optional<Dimension>> exportSizeAnswers = new ArrayDeque<>();
    final Deque<Boolean> confirmations = new ArrayDeque<>();
    String suggestedName;

    @Override
    public void showTitle(String newTitle) {
        title = newTitle;
    }

    @Override
    public void showSettings(FractalSettings newFractal, ColorSettings newColors, Location newLocation, boolean auto) {
        fractal = newFractal;
        colors = newColors;
        location = newLocation;
        autoIterations = auto;
    }

    @Override
    public void showPicture(BufferedImage newPicture) {
        picture = newPicture;
        pictures.add(newPicture);
    }

    @Override
    public void showStatus(String text) {
        status = text;
    }

    @Override
    public void showPointer(String text) {
        pointer = text;
    }

    @Override
    public void showZoom(String text) {
        zoom = text;
    }

    @Override
    public void showProgress(OptionalDouble fraction) {
        progress = fraction;
        progressVisible = true;
    }

    @Override
    public void hideProgress() {
        progressVisible = false;
    }

    @Override
    public void updateCommands(Commands newCommands) {
        commands = newCommands;
    }

    @Override
    public Dimension canvasSize() {
        return canvas;
    }

    @Override
    public Optional<Path> chooseParameterFile() {
        return openAnswers.isEmpty() ? Optional.empty() : openAnswers.pop();
    }

    @Override
    public Optional<Path> chooseParameterSavePath(String name) {
        suggestedName = name;
        return parameterSaveAnswers.isEmpty() ? Optional.empty() : parameterSaveAnswers.pop();
    }

    @Override
    public Optional<SaveTarget> chooseImageSavePath(String name) {
        suggestedName = name;
        return imageSaveAnswers.isEmpty() ? Optional.empty() : imageSaveAnswers.pop();
    }

    @Override
    public Optional<Dimension> chooseExportSize(Dimension suggested) {
        return exportSizeAnswers.isEmpty() ? Optional.empty() : exportSizeAnswers.pop();
    }

    @Override
    public void copyToClipboard(BufferedImage image) {
        clipboardImage = image;
    }

    @Override
    public void copyToClipboard(String text) {
        clipboardText = text;
    }

    @Override
    public void setCycling(boolean on) {
        cycling = on;
    }

    @Override
    public void showBar(Bar bar, boolean visible) {
        bars.put(bar, visible);
    }

    @Override
    public boolean confirm(String message, String title) {
        return confirmations.isEmpty() || confirmations.pop();
    }

    @Override
    public void showError(String message) {
        errors.add(message);
    }

    @Override
    public void showInfo(String message) {
        infos.add(message);
    }

    @Override
    public void showAbout() {
        infos.add("about");
    }

    @Override
    public void dispose() {
        disposed = true;
    }
}
