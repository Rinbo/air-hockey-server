package nu.borjessons.airhockeyserver.game.properties;

/**
 * The radius as expressed as a percentage of board width and height.
 * The constituent parts x and y are not independent.
 * h = board height
 * w = board width
 * ar = aspect ratio (w / h)
 * c = radius absolute value
 * <p>
 * x = c / w
 * y = c / h
 * c = y * h
 * c = x * w
 * x * w = y * h
 * x = y * (h / w) => x = y / ar
 * y = x * (w / h) => y = x * ar
 */
public record Radius(double x, double y) {
}

/*
What do I want to do?
I want to determine if a handle and puck has collided.
What do I need to know to make this determination?
I need the position of the puck and the handle.
The positions are expressed as percentages of board width and height
By subtracting the constituent percentages of the puck by those of the handle I get the difference in height and width as a percentage
So now I could theoretically convert these to real number, and I can always get the circle radi as an absolute number.
If I have those I can calculate the distance between the center lines of the circles and compare that to the sum of the radi
But something happens in the calculation of the distance between the center points?
I am merging a two-dimensional problem into a one-dimensional one? The distance calculated is a percentage of what? Height or width?
Depends what I compare it to?
When I calculate the distance, if I divide y-percentage by ar it gives me the percentage as a function of width!
I have to pick either width or height when I work with length!
 */
