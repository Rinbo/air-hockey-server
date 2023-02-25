package nu.borjessons.airhockeyserver.game;

import nu.borjessons.airhockeyserver.game.objects.Handle;
import nu.borjessons.airhockeyserver.game.objects.Puck;

public record BoardState(Puck puck, Handle playerOne, Handle playerTwo) {
}
