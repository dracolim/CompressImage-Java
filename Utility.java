import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.io.*;
import javax.imageio.ImageIO;

public class Utility implements Serializable {

    int originalImageHeight;
    int originalImageWidth;

    /* QuadTree start */
    public class Quadtree implements Serializable {
        // can edit
        private static final double ERROR_THRESHOLD = 6.0;
        private static final int OUTPUT_SCALE = 1;
        private static final int PADDING = 0;

        private QuadtreeNode root;
        private int width;
        private int height;
        private int maxDepth = 1024; 

        public Quadtree(BufferedImage image, int maxDepth) {
            this.root = new QuadtreeNode(image, new int[] { 0, 0, image.getWidth(), image.getHeight() }, 0);
            this.width = image.getWidth();
            this.height = image.getHeight();
            this.maxDepth = 0;

            buildTree(image, root, maxDepth);
        }

        private void buildTree(BufferedImage image, QuadtreeNode node, int maxDepth) {
            if (node.depth >= maxDepth || node.error <= ERROR_THRESHOLD) {
                if (node.depth > this.maxDepth) {
                    this.maxDepth = node.depth;
                }

                node.isLeaf = true;
                return;
            }

            node.split(image);
            for (QuadtreeNode child : node.children) {
                buildTree(image, child, maxDepth);
            }
        }

        // get all nodes on a given depth (BFS)
        public List<QuadtreeNode> getLeafNodes(int depth) {
            if (depth > this.maxDepth) {
                throw new IllegalArgumentException("A depth larger than the tree's depth was given");
            }

            List<QuadtreeNode> leafNodes = new ArrayList<>();
            getLeafNodesRecursion(this, root, depth, leafNodes);
            return leafNodes;
        }

        // recursively get leaf nodes based on whther a node is a leaf or the given depth is reached
        private void getLeafNodesRecursion(Quadtree tree, QuadtreeNode node, int depth, List<QuadtreeNode> leafNodes) {
            if (node.isLeaf || node.depth == depth) {
                leafNodes.add(node);
            } else if (node.children != null) {
                for (QuadtreeNode child : node.children) {
                    getLeafNodesRecursion(tree, child, depth, leafNodes);
                }
            }
        }

        private BufferedImage createImageFromDepth(int depth) {
            int m = OUTPUT_SCALE;
            int dx = PADDING;
            int dy = PADDING;
            BufferedImage image = new BufferedImage(this.width * m + dx, this.height * m + dy,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.BLACK);
            graphics.fillRect(0, 0, this.width * m + dx, this.height * m + dy);

            List<QuadtreeNode> leafNodes = getLeafNodes(depth);
            for (QuadtreeNode node : leafNodes) {
                int x = node.left * m + dx;
                int y = node.top * m + dy;
                int w = node.width * m;
                int h = node.height * m;
                graphics.setColor(new Color(node.color[0], node.color[1], node.color[2]));
                graphics.fillRect(x, y, w, h);
            }
            return image;
        }

        public BufferedImage renderAtDepth(int depth) {

            if (depth > this.maxDepth) {
                throw new IllegalArgumentException("A depth larger than the tree's depth was given");
            }
            BufferedImage image = createImageFromDepth(depth);
            return image;
        }
    }

    public class QuadtreeNode implements Serializable {
        private int left, top, right, bottom;
        private int depth;
        private QuadtreeNode[] children;
        private boolean isLeaf;
        private int[] color;
        private double error;
        private int width, height;

        public QuadtreeNode(BufferedImage img, int[] box, int depth) {
            this.left = box[0];
            this.top = box[1];
            this.right = box[2];
            this.bottom = box[3];
            this.depth = depth;
            this.isLeaf = false;
            this.children = null;
            this.width = right - left;
            this.height = bottom - top;

            // Get the node's average color
            if (width > 0 && height > 0) {
                BufferedImage subImage = img.getSubimage(left, top, width, height);
                try {
                    Map<String, int[]> colorHistograms = ColorHistogram.createColorHistogram(subImage);
                    List<Integer> histogram = ColorHistogram.hist(colorHistograms);
                    List<Object> result = ColorHistogram.colourFromHistogram(histogram);

                    int[] rgb = (int[]) result.get(0);
                    double error = (double) result.get(1);
                    this.color = rgb;
                    this.error = error;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public boolean isLeaf() {
            return isLeaf;
        }

        public void split(BufferedImage img) {
            double lr = left + (right - left) / 2;
            double tb = top + (bottom - top) / 2;

            int[] tlBox = { left, top, (int) lr, (int) tb };
            int[] trBox = { (int) lr, top, right, (int) tb };
            int[] blBox = { left, (int) tb, (int) lr, bottom };
            int[] brBox = { (int) lr, (int) tb, right, bottom };

            QuadtreeNode tl = new QuadtreeNode(img, tlBox, depth + 1);
            QuadtreeNode tr = new QuadtreeNode(img, trBox, depth + 1);
            QuadtreeNode bl = new QuadtreeNode(img, blBox, depth + 1);
            QuadtreeNode br = new QuadtreeNode(img, brBox, depth + 1);

            this.children = new QuadtreeNode[] { tl, tr, bl, br };
            this.isLeaf = false;
        }
    }

    public class ColorHistogram {

        public static Map<String, int[]> createColorHistogram(BufferedImage image) throws IOException {

            // Get the image's dimensions
            int width = image.getWidth();
            int height = image.getHeight();

            // Initialize color channel arrays to count pixel frequencies
            int[] redHistogram = new int[256];
            int[] greenHistogram = new int[256];
            int[] blueHistogram = new int[256];

            // Iterate through each pixel in the image
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int rgb = image.getRGB(x, y);

                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;

                    // Update the color histograms
                    redHistogram[red]++;
                    greenHistogram[green]++;
                    blueHistogram[blue]++;
                }
            }

            // Create a map to organize the histograms by color channel
            Map<String, int[]> colorHistograms = new HashMap<>();
            colorHistograms.put("red", redHistogram);
            colorHistograms.put("green", greenHistogram);
            colorHistograms.put("blue", blueHistogram);

            return colorHistograms;
        }

        public static List<Object> colourFromHistogram(List<Integer> histogram) throws IOException {
            List<Integer> red = histogram.subList(0, 256);
            List<Integer> greeen = histogram.subList(256, 512);
            List<Integer> blue = histogram.subList(512, 768);

            ArrayList<Double> redWeightedAverage = getWeightedAverage(red);
            ArrayList<Double> greenWeightedAverage = getWeightedAverage(greeen);
            ArrayList<Double> blueWeightedAverage = getWeightedAverage(blue);
            int r = (int) Math.round(redWeightedAverage.get(0));
            int g = (int) Math.round(greenWeightedAverage.get(0));
            int b = (int) Math.round(blueWeightedAverage.get(0));

            Double error = redWeightedAverage.get(1) * 0.2989 + greenWeightedAverage.get(1) * 0.5870
                    + blueWeightedAverage.get(1) * 0.1140;

            List<Object> result = new ArrayList<>();
            result.add(new int[] { r, g, b });
            result.add(error);

            return result;
        }

        // Combined R, G, B together as one array
        public static List<Integer> hist(Map<String, int[]> histogram_dict) throws IOException {
            List<Integer> histogram = new ArrayList<Integer>();
            for (int i = 0; i < 256; i++) {
                histogram.add(histogram_dict.get("red")[i]);
            }
            for (int i = 0; i < 256; i++) {
                histogram.add(histogram_dict.get("green")[i]);
            }
            for (int i = 0; i < 256; i++) {
                histogram.add(histogram_dict.get("blue")[i]);
            }
            return histogram;
        }

        // get weighted average
        public static ArrayList<Double> getWeightedAverage(List<Integer> subHistogram) throws IOException {
            ArrayList<Double> weightedAverage = new ArrayList<Double>();

            int total = subHistogram.stream().mapToInt(Integer::intValue).sum();
            double value = 0;
            double error = 0;

            if (total > 0) {
                for (int i = 0; i < subHistogram.size(); i += 1) {
                    int x = subHistogram.get(i);
                    value += (i * x);
                }
                value = value / total;

                for (int i = 0; i < subHistogram.size(); i += 1) {
                    int x = subHistogram.get(i);
                    error += (x * Math.pow(value - i, 2));
                }

                error = error / total;
                error = Math.sqrt(error);
            }

            weightedAverage.add(value);
            weightedAverage.add(error);

            return weightedAverage;
        }
    }

    /* QuadTree end */

    /* Compress start */
    public void Compress(int[][][] pixels, String outputFileName) throws IOException {
        final ExecutorService executor = Executors.newFixedThreadPool(4);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputFileName))) {

            BufferedImage image = convertPixelsToImage(pixels);
            this.originalImageHeight = image.getHeight();
            this.originalImageWidth = image.getWidth();

            // Divide the image into non-overlapping parts
            BufferedImage[] parts = divideImage(image);

            // Create a list to hold the Future objects
            List<Future<BufferedImage>> futures = new ArrayList<>();

            // Submit the tasks to the executor
            for (BufferedImage part : parts) {
                futures.add(executor.submit(new Callable<BufferedImage>() {
                    @Override
                    public BufferedImage call() throws Exception {
                        int maxDepth = calculateMaxDepth(part);
                        // EDIT FIRST ARGUEMENT BELOW THIS
                        int depth = Math.min(8, maxDepth);
                        Quadtree tree = new Quadtree(part, 1024);
                        BufferedImage compressedPart = tree.renderAtDepth(depth);
                        return compressedPart;
                    }
                }));
            }

            // Collect the results
            BufferedImage[] compressedParts = new BufferedImage[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try {
                    compressedParts[i] = futures.get(i).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            // Combine the compressed parts into a single image
            BufferedImage finalCompressedImage = combineParts(compressedParts);

            // Write the final compressed image to the output stream
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(finalCompressedImage, "jpg", bos);
            byte[] data = bos.toByteArray();
            oos.writeObject(data);
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            executor.shutdown();
        }
    }

    // convert pixels to image
    public BufferedImage convertPixelsToImage(int[][][] pixelData) {
        int width = pixelData.length;
        int height = pixelData[0].length;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int red = pixelData[x][y][0];
                int green = pixelData[x][y][1];
                int blue = pixelData[x][y][2];
                int rgb = (red << 16) | (green << 8) | blue; // Create an RGB color from the components
                image.setRGB(x, y, rgb);
            }
        }

        return image;
    }

    public BufferedImage[] divideImage(BufferedImage image) {
        int cols = 2; // number of columns for the grid
        int rows = 2; // number of rows for the grid

        int partWidth = image.getWidth() / cols;
        int partHeight = image.getHeight() / rows;


        BufferedImage[] parts = new BufferedImage[cols * rows];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // Create a subimage for each part
                parts[i * cols + j] = image.getSubimage(j * partWidth, i * partHeight, partWidth, partHeight);
            }
        }

        return parts;
    }

    public BufferedImage combineParts(BufferedImage[] parts) {
        int rows = 2; // number of rows for the grid
        int cols = 2; // number of columns for the grid

        // Determine the dimensions of the final image
        int maxPartWidth = 0;
        int maxPartHeight = 0;

        for (BufferedImage part : parts) {
            maxPartWidth = Math.max(maxPartWidth, part.getWidth());
            maxPartHeight = Math.max(maxPartHeight, part.getHeight());
        }

        int width = this.originalImageWidth;
        int height = this.originalImageHeight;

        // Create a new image of the correct size
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = combined.getGraphics();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int index = i * cols + j;
                if (index < parts.length) {
                    BufferedImage part = parts[index];
                    int x = j * maxPartWidth;
                    int y = i * maxPartHeight;

                    // Draw the part onto the combined image
                    g.drawImage(part, x, y, null);
                }
            }
        }

        return combined;
    }

    public int calculateMaxDepth(BufferedImage image) {
        // Assuming the image is square
        int size = Math.max(image.getWidth(), image.getHeight());

        // Calculate the log base 2 of the size
        int maxDepth = (int) (Math.log(size) / Math.log(2));

        return maxDepth;
    }
    /* Compress end */

    /* Decompress start */
    public int[][][] Decompress(String inputFileName) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(inputFileName))) {
            Object object = ois.readObject();

            if (object instanceof byte[]) {

                BufferedImage decodedImage = decodeImage((byte[]) object);
                int[][][] pixelData = ImageToPixels(decodedImage);

                return (int[][][]) pixelData;
            } else {
                throw new IOException("Invalid object type in the input file");
            }
        }
    }

    public BufferedImage decodeImage(byte[] imageData) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(imageData);
            BufferedImage image = ImageIO.read(bis);

            bis.close();
            return image;
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Handle the exception according to your requirements
        }
    }

    public int[][][] ImageToPixels(BufferedImage image) {
        // Get image dimensions
        int width = image.getWidth();
        int height = image.getHeight();

        // Create a 3D array to store RGB values for each pixel
        int[][][] pixelData = new int[width][height][3]; // 3 for R, G, B components

        // Iterate through each pixel and get its RGB values
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);

                // Extract the red, green, and blue components
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                // Store the RGB values in the pixelData array
                pixelData[x][y][0] = red;
                pixelData[x][y][1] = green;
                pixelData[x][y][2] = blue;
            }
        }

        return pixelData;
    }
    /* Decompress end */

}
