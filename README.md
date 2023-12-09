# Compress Image using QuadTree in Java
[![Build In](https://travis-ci.org/joemccann/dillinger.svg?branch=master)](https://travis-ci.org/joemccann/dillinger)

### Why Quadtree? 🌳 
- Reduce the size of an image without necessarily reducing the image quality
- Speeds up the edge detection in images, as it only focuses on the detailed section of the image
- Effective in efficient storage

### How are they used in Image compression? 
- Works by recursively dividing the image into 4 sub-spaces with each holding the average RGB color + the error that determines that color for its subspaces
- The threshold is set based on that error and it helps the tree to determine if a node should be split further or not

### To run Code
- copy and paste ```javac *.java``` into your terminal 
- Run the App.java by entering ```java App.java``` into the same terminal

## In to Depth of the Quadtree structure ↕️
### Quadtree Node 
- Images are divided into smaller regions by recursively splitting them into four equal sized quadrants
- Each node in the quadtree represents one of these quadrants
- Information such as dimensions, colour, error are kept for each node

<img width="225" alt="Screenshot 2023-12-09 at 6 00 45 AM" src="https://github.com/dracolim/CompressImage-Java/assets/85498185/a69fbfb0-59af-4224-86ef-e3f727bfc2ea">

### Quadtree - Colour Histogram
- Colour Histogram is build by iterating through each pixel of the image, extracting the RGB components, and updating the histogram for each colour channel
- Purpose: It provides insights into the colour composition and it is essential as a basis for compression decisions

<img width="245" alt="Screenshot 2023-12-09 at 6 03 17 AM" src="https://github.com/dracolim/CompressImage-Java/assets/85498185/f814b170-e7ab-4394-a060-860ebdbc0ecf">

### Quadtree - Weighted Average
- Calculate the weighted averages for each colour channels (subHistogram parameter) using the colour histogram 
- Calculates the error which is a standard deviation of pixel intensities from the average value
- Returns: ArrayList<Double> = [WA value , error]
- Purpose: averages is crucial as it determines the dominant colours in an image and identifies the most important colours for preserving image quality
By assigning weights based on their significance, it can effectively reduce the colour space, which is essential for image compression

### Quadtree structure
- Core logic of Quadtree: Depth first search Recursively constructs the quadtree from an input image and determines whether to split a node further based on: 
1. Max Depth
2. Error threshold

#### Max Depth
- The Max depth dictates the maximum number of levels or divisions within the quadtree structure
- Each level represents a further subdivision of the image into smaller quadrants
- Maximum depth has a direct impact on the level of detail reserved in the compressed image 
- Lower Max Depth will produce a coarser representation of the image, reducing the size but potentially sacrificing fine details

#### Error Threshold
- The error threshold determines how much error or difference from the original image is acceptable in the compressed representation. Higher error threshold => Greater difference from the original image , means more detail is preserved
- During the compression process, higher error threshold will also mean that the process will stop splitting the region earlier, resulting in larger regions and less aggressive reduction of image data
- Since larger regions are preserved, the compressed image contains more detail and closely resembles the original image leading the better image quality 

## Optimize the Process
### Utilize ExecutorService - speed up the image compression process
Utilized ExecutorService in parallelizing image compression. With a fixed thread pool, we ensure that up to four tasks can be executed simultaneously, significantly improving performance.

## For more in detailed explaination
[Quadtree Video](https://youtu.be/2BvFSd4kBq8) 



