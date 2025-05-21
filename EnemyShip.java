import java.awt.*;

public class EnemyShip {
    int x, y;
    int width = 40;
    int height = 40;
    int speed = 2;

    public EnemyShip(int startX, int startY) {
        this.x = startX;
        this.y = startY;
    }

    public void update(int playerX) {
        if (x < playerX) x += speed;
        else if (x > playerX) x -= speed;
        y += speed;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics g) {
        g.setColor(Color.RED);
        g.fillRect(x, y, width, height);
    }
}