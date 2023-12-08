# Compress Image using QuadTree in Java

[![Build Status](https://travis-ci.org/joemccann/dillinger.svg?branch=master)](https://travis-ci.org/joemccann/dillinger)

### Why Quadtree? 🌳 
- Reduce the size of an image without necessarily reducing the image quality
- Speeds up the edge detection in images, as it only focuses on the detailed section of the image
- Effective in efficient storage

### How are they used in Image compression? 
- Works by recursively dividing the image into 4 sub-spaces with each holding the average RGB color + the error that determines that color for its subspaces
- The threshold is set based on that error and it helps the tree to determine if a node should be split further or not

## In to Depth of the Quadtree structure
### Quadtree Node 
- Images are divided into smaller regions by recursively splitting them into four equal sized quadrants
- Each node in the quadtree represents one of these quadrants
- Information such as dimensions, colour, error are kept for each node

<img width="225" alt="Screenshot 2023-12-09 at 6 00 45 AM" src="https://github.com/dracolim/CompressImage-Java/assets/85498185/a69fbfb0-59af-4224-86ef-e3f727bfc2ea">
