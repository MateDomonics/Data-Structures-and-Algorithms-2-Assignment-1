package com.matedomonics.assignment1;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class PCBController {
    int[] disjointArray;
    HashMap<Integer, ArrayList<Integer>> disjointHash;

    static Image displayedImage;
    private File file;
    double hueMod = 20;
    double brightMod = 0.15;
    double satMod = 0.15;
    List<Color> colours = new ArrayList<>();
    int width;
    int height;
    double currentNoise;
    Random random = new Random();

    @FXML
    public ImageView imageView, processedView;
    @FXML
    public Circle circle;
    @FXML
    public Label colourLabel, noiseAmount, componentNumber;
    @FXML
    public WritableImage writtenImage;
    @FXML
    public Slider noiseSlider;
    @FXML
    public TextField componentTypeField;
    @FXML
    public CheckBox checkBox;

    //Necessary for the slider listener to work.
    @FXML
    private void initialize() {
        noiseSliderListener();
    }

    @FXML
    public void openImage(ActionEvent event) throws IOException {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Extensions", "*.JPG", "*.PNG", "*.JPEG"); //Filter file types to only allow pictures
        fileChooser.getExtensionFilters().addAll(filter);

        fileChooser.setTitle("Open an Image");
        file = fileChooser.showOpenDialog(new Stage());
        displayedImage = new Image(file.toURI().toString(), imageView.getFitWidth(), imageView.getFitHeight(), true, true);
        imageView.setImage(displayedImage);
    }

    @FXML
    public void colourDebug(MouseEvent event) {
        PixelReader pixelReader = imageView.getImage().getPixelReader();
        Color color = pixelReader.getColor((int) event.getX(), (int) event.getY());

        //Clear the saved colors if the checkbox is not selected
        if (!checkBox.isSelected()) {
            colours.clear();
        }

        //Save the specified pixel's colour into an array.
        colours.add(color);

        //Fill a circle with the colour of the picked pixel. Also display hex value.
        circle.setFill(color);
        circle.setStrokeWidth(1);
        colourLabel.setText(color.toString());

        System.out.println("\nPixel location X: " + (int) event.getX() + "\nPixel location Y: " + (int) event.getY());
        System.out.println("\nHue: " + color.getHue() + "\nSaturation: " + color.getSaturation() + "\nBrightness: " + color.getBrightness());
        System.out.println("\nRed: " + color.getRed() + "\nGreen: " + color.getGreen() + "\nBlue: " + color.getBlue());
        System.out.println("\nNumber of colors held selected: " + colours.size());

        processAndShow();
    }

    //Calls all methods, and is called above when a colour is picked to improve usability.
    private void processAndShow() {
        imageProcessing();
        pixelGrouping();
        noiseSuppression();
        drawRectangle();
    }

    //For ease of reading, a separate call for the slider is made.
    @FXML
    public void dragReleased(MouseEvent mouseEvent) {
        processAndShow();
    }

    private void imageProcessing() {
        PixelReader pixelReader = imageView.getImage().getPixelReader();
        width = (int) imageView.getImage().getWidth();
        height = (int) imageView.getImage().getHeight();
        WritableImage writableImage = new WritableImage(width, height);
        disjointArray = new int[width * height];

        for (Color colour : colours) {
            double increasedHue = colour.getHue() + hueMod;
            double decreasedHue = colour.getHue() - hueMod;

            double increasedSat = colour.getSaturation() + satMod;
            double decreasedSat = colour.getSaturation() - satMod;

            double increasedBright = colour.getBrightness() + brightMod;
            double decreasedBright = colour.getBrightness() - brightMod;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    //Get every pixel's hue, sat and bright.
                    double currentHue = pixelReader.getColor(x, y).getHue();
                    double currentSat = pixelReader.getColor(x, y).getSaturation();
                    double currentBright = pixelReader.getColor(x, y).getBrightness();
                    //Add a unique value for every pixel to the array.
                    int indexIn1D = (y * width) + x;

                    //Call method below to check for hue, then check for brightness and saturation.
                    if (hueOutlier(currentHue, increasedHue, decreasedHue) &&
                            ((currentBright >= decreasedBright) && (currentBright <= increasedBright)) &&
                            ((currentSat >= decreasedSat) && (currentSat <= increasedSat))) {
                        writableImage.getPixelWriter().setColor(x, y, new Color(0, 0, 0, 1));
                        disjointArray[indexIn1D] = indexIn1D;
                    } else if (disjointArray[indexIn1D] == 0) {
                        writableImage.getPixelWriter().setColor(x, y, new Color(1, 1, 1, 1));
                        //Set the unique value to -1
                        disjointArray[indexIn1D] = -1;
                    }
                }
            }
        }
        writtenImage = writableImage;
        processedView.setImage(writableImage);

    }

    private boolean hueOutlier(double currentHue, double increasedHue, double decreasedHue) {
        boolean over = increasedHue > 360;
        boolean below = decreasedHue < 1;
        if (!over && !below) {
            //Hue is not an outlier, so check for a range as usual (above and below).
            return currentHue >= decreasedHue && currentHue <= increasedHue;
        } else if (below) {
            //If less than 1, check if it's between 1 and increasedHue, or if it's greater than 360 + decreasedHue (which is a negative number)
            return currentHue <= increasedHue || currentHue >= (360 + decreasedHue);
        } else {
            //If greater than 360, check if it's between 360 and decreasedHue,
            //or if it's less than the increasedHue (which is above 360) minus 360 + 1 (to make sure we don't end up with 0 hue)
            return currentHue >= decreasedHue || currentHue <= (increasedHue - 360);
        }
    }

    //Union-find changes the 2D image to a 1D ArrayList.
    private void union(int[] a, int p, int q) {
        a[find(a, q)] = find(a, p);
    }

    private int find(int[] a, int root) {
        if (a[root] < 0) return a[root];
        if (a[root] == root) {
            return root;
        } else {
            return find(a, a[root]);
        }
    }

    public void pixelGrouping() {
        disjointHash = new HashMap<>();
        //Go through each number in the ArrayList.
        for (int i = 0; i < disjointArray.length; i++) {
            if (disjointArray[i] >= 0) {
                //If the current number's modulo is not equal to 0, compared to the width of the image (implying that we haven't reached the right-most value)
                //and if the next number does not equal to -1 (i.e. white), add the current value + the value on the right to the ArrayList.
                if (((i + 1) % width != 0) && (disjointArray[i + 1] != -1)) {
                    union(disjointArray, i, i + 1);
                }
                //If the current number + the width is less than the length of the array (enforcing that we do not go out of the bound of the picture),
                //and the number below the current number is not -1 (white), then add the current value + the value below to the ArrayList.
                if (((i + width) < disjointArray.length) && (disjointArray[i + width] != -1)) {
                    union(disjointArray, i, i + width);
                }
            }
        }


        for (int i = 0; i < disjointArray.length; i++) {
            //If the current number is not -1 (white)
            if (disjointArray[i] != -1) {
                //then we find the real root (the current number)
                int realRoot = find(disjointArray, disjointArray[i]);
                //If the HashMap already contains the real root as a key
                if (disjointHash.containsKey(realRoot)) {
                    //Then find the ArrayList that is connected to the realRoot and add the value to it.
                    ArrayList<Integer> disjointHashValues = disjointHash.get(realRoot);
                    disjointHashValues.add(i);
                } else {
                    //If it doesn't exist, then create a new ArrayList, add the values to it and make the realRoot the key, and the disjointHashValues the ArrayList connected to it.
                    ArrayList<Integer> disjointHashValues = new ArrayList<>();
                    disjointHashValues.add(i);
                    disjointHash.put(realRoot, disjointHashValues);
                }
            }
        }

        System.out.println("Disjoint Sets: " + disjointHash.size());
    }

    private void noiseSuppression() {
        width = (int) imageView.getImage().getWidth();
        height = (int) imageView.getImage().getHeight();
        WritableImage writableImage = writtenImage;

        //Get the current value of the slider.
        currentNoise = noiseSlider.getValue();
        //Go through each key in the Hash Map
        for (Integer key : disjointHash.keySet()) {
            //If the size of the Key's values is smaller than the value of the slider,
            if (disjointHash.get(key).size() <= currentNoise) {
                //Then go through the values, get their x and y coordinates and set them to white.
                for (int i = 0; i < disjointHash.get(key).size(); i++) {
                    int column = disjointHash.get(key).get(i) % width;
                    int row = disjointHash.get(key).get(i) / width;
                    writableImage.getPixelWriter().setColor(column, row, Color.WHITE);
                }
            }
        }
        processedView.setImage(writtenImage);
        disjointHash.keySet().removeIf(row -> disjointHash.get(row).size() <= currentNoise);
        System.out.println("Current Noise Reduction: " + currentNoise);
        System.out.println("Amount of Disjoint Sets: " + disjointHash.size());
        componentNumber.setText(String.valueOf(disjointHash.size()));
    }

    //Due to the limitations with labels and sliders, a stackoverflow page was suggested to me.
    //https://stackoverflow.com/questions/40593284/how-can-i-define-a-method-for-slider-change-in-controller-of-a-javafx-program
    private void noiseSliderListener() {
        noiseSlider.valueProperty().addListener((observable, noiseOld, noiseNew) -> {
            noiseAmount.setText(Integer.toString(noiseNew.intValue()));
        });
    }

    private void drawRectangle() {
        //If Rectangles exist, then remove them.
        ((Pane) imageView.getParent()).getChildren().removeIf(x -> x instanceof Rectangle || x instanceof Text);
        PixelReader pixelReader = imageView.getImage().getPixelReader();
        //Sort the Hash Map and store it in a variable (call the method below)
        ArrayList<Integer> sorted = getSortedKeys();
        //Go through each key in the Hash Map
        for (int root : disjointHash.keySet()) {
            //Save the values in the ArrayList for each Key (root)
            ArrayList<Integer> values = disjointHash.get(root);
            int startingX, startingY, endingX, endingY;
            //Set the Starting Values to the maximum possible integer value for ease of declaration (everything will be smaller than this)
            //This is necessary to get started on the comparisons of x and y positions.
            startingX = startingY = Integer.MAX_VALUE;
            endingX = endingY = Integer.MIN_VALUE;

            for (int elements : values) {
                int column = elements % width;
                int row = elements / width;
                if (startingX > column) {
                    startingX = column;
                }
                if (startingY > row) {
                    startingY = row;
                }
                if (endingX < column) {
                    endingX = column;
                }
                if (endingY < row) {
                    endingY = row;
                }
            }
            //Create the rectangle and set its starting position and attributes (e.g. Transparent fill, blue outline)
            Rectangle rectangle = new Rectangle(startingX, startingY, (endingX - startingX), (endingY - startingY));
            Color color = pixelReader.getColor(startingX - (endingX - startingY) / 2, startingY - (endingY - startingY) / 2);

            rectangle.setFill(Color.TRANSPARENT);
            rectangle.setStroke(Color.BLUE);
            rectangle.setStrokeWidth(3);

            rectangle.setTranslateX(imageView.getLayoutX());
            rectangle.setTranslateY(imageView.getLayoutY());

            //Create a Font
            Font font = Font.font("Times New Roman", FontWeight.BOLD, 17);
            Text text = new Text();
            text.setLayoutX(rectangle.getX());
            text.setLayoutY(rectangle.getY() + (endingY - startingY));
            text.setTranslateX(imageView.getLayoutX());
            text.setTranslateY(imageView.getLayoutY());
            text.setFont(font);
            text.setText(String.valueOf(sorted.indexOf(root) + 1));

            if (color.getBrightness() < 0.6) {
                text.setFill(Color.WHITE);
            }
            Tooltip tooltip = new Tooltip(componentTypeField.getText() + "\nComponent Number: " + sorted.indexOf(root) + 1
                    + "\nComponent size: " + disjointHash.get(root).size());
            Tooltip.install(rectangle, tooltip);

            ((Pane) imageView.getParent()).getChildren().add(rectangle);
            ((Pane) imageView.getParent()).getChildren().add(text);
        }
    }

    //Sort through the Keys of the HashMap using a Selection Sort
    private ArrayList<Integer> getSortedKeys() {
        //Save the keys of the HashMap in a separate ArrayList to save on processing power.
        ArrayList<Integer> sorted = new ArrayList<>(disjointHash.keySet());
        //i goes through the ArrayList from the start, x goes through from the end, until x reaches i.
        for (int i = 0; i < sorted.size() - 1; i++) {
            for (int x = sorted.size() - 1; x > i; x--) {
                int size1 = disjointHash.get(sorted.get(i)).size();
                int size2 = disjointHash.get(sorted.get(x)).size();
                if (size2 > size1) {
                    //Save the smaller variable temporarily
                    int temp = sorted.get(i);
                    //Replace the smaller value with the larger value
                    sorted.set(i, sorted.get(x));
                    //Place the smaller value back to where the larger value used to be
                    sorted.set(x, temp);
                }
            }
        }
        return sorted;
    }

    @FXML
    public void sampledColour(ActionEvent actionEvent) {
        PixelReader pixelReader = processedView.getImage().getPixelReader();
        PixelReader pixelReader2 = imageView.getImage().getPixelReader();
        WritableImage writableImage = new WritableImage(width, height);

        //Go through each pixel in the view and get the colour of each.
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                //If the colours are less than 1 (i.e. Black), then set them to the colour that was clicked on previously.
                if (pixelReader.getColor(x, y).getRed() < 1 && pixelReader.getColor(x, y).getBlue() < 1 && pixelReader.getColor(x, y).getGreen() < 1) {
                    writableImage.getPixelWriter().setColor(x, y, pixelReader2.getColor(x, y));
                } else {
                    //Otherwise, set it to white.
                    writableImage.getPixelWriter().setColor(x, y, Color.WHITE);
                }
            }
        }
        processedView.setImage(writableImage);
    }

    @FXML
    public void randomColour(ActionEvent actionEvent) {
        WritableImage writableImage = new WritableImage(width, height);

        //Set all the pixels to white.
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                writableImage.getPixelWriter().setColor(x, y, Color.WHITE);
            }
        }

        //Go through each key in the Hash Map and store the values of each in an Array List
        for (int root : disjointHash.keySet()) {
            ArrayList<Integer> values = disjointHash.get(root);
            //Create the random colour that will be assigned to each.
            Color colour = Color.color(getRandomColour(), getRandomColour(), getRandomColour());

            //Go through each element in the Array List, determine its x and y position and set its colour to the random colour.
            for (int elements : values) {
                int column = elements % width;
                int row = elements / width;
                writableImage.getPixelWriter().setColor(column, row, colour);
            }
        }
        processedView.setImage(writableImage);
    }

    @FXML
    public void clearPicture(ActionEvent actionEvent) {
        ((Pane) imageView.getParent()).getChildren().removeIf(x -> x instanceof Rectangle || x instanceof Text);
        writtenImage = new WritableImage(width, height);
        processedView.setImage(writtenImage);
        disjointArray = new int[0];
        disjointHash.clear();
        colours.clear();

        circle.setFill(Color.TRANSPARENT);
        circle.setStrokeWidth(0);
        colourLabel.setText("");

        componentNumber.setText("");
    }

    //Generate a random integer that is then converted to a double.
    private double getRandomColour() {
        int randomNumber = random.nextInt(256);
        //https://stackoverflow.com/questions/5731863/mapping-a-numeric-range-onto-another
        //Transforms the 0-255 number to a number between 0-1 (necessary for setting the random colour)
        return 0.0 + (1.0 / 255 * randomNumber);
    }

    @FXML
    public void exitApplication(ActionEvent event) {
        System.exit(0);
    }
}