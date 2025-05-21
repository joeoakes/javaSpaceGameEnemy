import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class SpaceGame extends JFrame implements KeyListener {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    private static final int PLAYER_WIDTH = 50;
    private static final int PLAYER_HEIGHT = 50;
    private static final int OBSTACLE_WIDTH = 50;
    private static final int OBSTACLE_HEIGHT = 50;
    private static final int PROJECTILE_WIDTH = 5;
    private static final int PROJECTILE_HEIGHT = 10;
    private static final int PLAYER_SPEED = 5;
    private static final int OBSTACLE_SPEED = 3;
    private static final int PROJECTILE_SPEED = 10;
    private int score = 0;

    private JPanel gamePanel;
    private JLabel scoreLabel;
    private Timer timer;
    private boolean isGameOver;
    private int playerX, playerY;
    private int projectileX, projectileY;
    private boolean isProjectileVisible;
    private boolean isFiring;
    //private java.util.List<Point> obstacles;
    private BufferedImage spriteSheet;
    private int spriteWidth = 64;
    private int spriteHeight = 64;

    private boolean shieldActive = false;
    private int shieldDuration = 5000; // Shield duration in milliseconds
    private long shieldStartTime;

    private List<Point> stars;
    private BufferedImage shipImage;
    private Clip clip;

    private GameState gameState = GameState.PLAYING;
    private PlayerState playerState = PlayerState.IDLE;

    private List<EnemyShip> enemies = new ArrayList<>();

    // Each obstacle has its own state
    private class Obstacle {
        Point position;
        ObstacleState state;

        Obstacle(int x, int y) {
            this.position = new Point(x, y);
            this.state = ObstacleState.FALLING;
        }
    }
    private List<Obstacle> obstacleList = new ArrayList<>();

    public SpaceGame() {

        try {
            // Load the ship image from file
            shipImage = ImageIO.read(new File("ship.png"));
            spriteSheet = ImageIO.read(new File("astro.png"));
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("fire.wav").getAbsoluteFile());
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
        } catch ( LineUnavailableException ex) {
            ex.printStackTrace();
        } catch (UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        stars = generateStars(200); // Generate 200 stars initially

        setTitle("Space Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.GREEN);
        scoreLabel.setBounds(10, 10, 100, 20);
        gamePanel.add(scoreLabel);

        add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 20;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;
        isProjectileVisible = false;
        isGameOver = false;
        isFiring = false;
        //obstacles = new java.util.ArrayList<>();

        timer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isGameOver) {
                    update();
                    gamePanel.repaint();
                }
            }
        });
        timer.start();
    }

    public void playSound() {
        if (clip != null) {
            clip.setFramePosition(0); // Rewind to the beginning
            clip.start(); // Start playing the sound
        }
    }

    private List<Point> generateStars(int numStars) {
        List<Point> starsList = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < numStars; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            starsList.add(new Point(x, y));
        }
        return starsList;
    }

    public static Color generateRandomColor() {
        Random rand = new Random();
        int r = rand.nextInt(256); // Red component (0-255)
        int g = rand.nextInt(256); // Green component (0-255)
        int b = rand.nextInt(256); // Blue component (0-255)
        return new Color(r, g, b);
    }

    private void draw(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        //Player
        g.drawImage(shipImage, playerX, playerY, null);
        //g.setColor(Color.BLUE);
        //g.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        if (isProjectileVisible) {
            g.setColor(Color.GREEN);
            g.fillRect(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        }

        for (EnemyShip enemy : enemies) {
            enemy.draw(g);
        }

        if (isShieldActive()) {
            g.setColor(new Color(0, 255, 255, 100)); // Semi-transparent cyan
            g.fillOval(playerX, playerY, 60, 60);
        }

        //g.setColor(Color.RED);
        //for (Point obstacle : obstacles) {
        //    g.fillRect(obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
        //}
        for (Obstacle obstacle : obstacleList) {
            if (spriteSheet != null && obstacle.state == ObstacleState.FALLING) {
                // Randomly select a sprite index (0-3)
                Random random = new Random();
                int spriteIndex = random.nextInt(4);

                // Calculate the x and y coordinates of the selected sprite on the sprite sheet
                int spriteX = spriteIndex * spriteWidth;
                int spriteY = 0; // Assuming all sprites are in the first row

                // Draw the selected sprite onto the canvas at the obstacle's position
                g.drawImage(
                        spriteSheet.getSubimage(spriteX, spriteY, spriteWidth, spriteHeight),
                        obstacle.position.x,
                        obstacle.position.y,
                        null
                );
            }
        }

        // Draw stars
        g.setColor(generateRandomColor());
        //g.setColor(Color.WHITE);
        for (Point star : stars) {
            g.fillOval(star.x, star.y, 2, 2);
        }

        if (isGameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Game Over!", WIDTH / 2 - 80, HEIGHT / 2);
        }
    }

    private void update() {
        if (!isGameOver) {
            // Move obstacles
            for (int i = 0; i < obstacleList.size(); i++) {
                Obstacle obs = obstacleList.get(i);
                if (obs.state == ObstacleState.FALLING) {
                    obs.position.y += OBSTACLE_SPEED;
                    if (obs.position.y > HEIGHT) {
                        obstacleList.remove(i--);
                    }
                }
            }


            for (int i = 0; i < enemies.size(); i++) {
                EnemyShip enemy = enemies.get(i);
                enemy.update(playerX);
                if (enemy.y > HEIGHT) enemies.remove(i--);
            }

            if (Math.random() < 0.01) { // Adjust frequency as needed
                int enemyX = new Random().nextInt(WIDTH - 40);
                enemies.add(new EnemyShip(enemyX, 0));
            }

            // Generate new obstacles Spawning
            if (Math.random() < 0.02) {
                int obstacleX = (int) (Math.random() * (WIDTH - OBSTACLE_WIDTH));
                obstacleList.add(new Obstacle(obstacleX, 0));
            }

            if (Math.random() < 0.1) {
                stars = generateStars(200); // Regenerate stars
            }

            // Move projectile
            if (isProjectileVisible) {
                projectileY -= PROJECTILE_SPEED;
                if (projectileY < 0) {
                    isProjectileVisible = false;
                }
            }

            // Check collision with player
            Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
            for (Obstacle obstacle : obstacleList) {
                Rectangle obstacleRect = new Rectangle(obstacle.position.x, obstacle.position.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                if (playerRect.intersects(obstacleRect) && !isShieldActive()) {
                    gameState = GameState.GAME_OVER;
                    break;
                }
            }

            // Check collision with obstacle
            Rectangle projectileRect = new Rectangle(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
            for (int i = 0; i < obstacleList.size(); i++) {
                Obstacle obstacle = obstacleList.get(i);
                Rectangle obstacleRect = new Rectangle(obstacle.position.x, obstacle.position.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
                if (projectileRect.intersects(obstacleRect)) {
                    obstacleList.remove(i--);
                    score += 10;
                    isProjectileVisible = false;
                    break;
                }
            }

            for (int i = 0; i < enemies.size(); i++) {
                EnemyShip enemy = enemies.get(i);
                if (projectileRect.intersects(enemy.getBounds())) {
                    enemies.remove(i--);
                    score += 20;
                    isProjectileVisible = false;
                }
            }
            scoreLabel.setText("Score: " + score);
        }
    }

    private void reset(){
        score = 0;
        isGameOver = false;
        gamePanel.repaint();

    }

    private void activateShield() {
        shieldActive = true;
        shieldStartTime = System.currentTimeMillis();
    }

    private void deactivateShield() {
        shieldActive = false;
    }

    private boolean isShieldActive() {
        return shieldActive && (System.currentTimeMillis() - shieldStartTime) < shieldDuration;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_LEFT && playerX > 0) {
            playerX -= PLAYER_SPEED;
            playerState = PlayerState.MOVING_LEFT;
        } else if (keyCode == KeyEvent.VK_RIGHT && playerX < WIDTH - PLAYER_WIDTH) {
            playerX += PLAYER_SPEED;
            playerState = PlayerState.MOVING_RIGHT;
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            reset();
        } else if (keyCode == KeyEvent.VK_CONTROL) {
            activateShield();
            playerState = PlayerState.SHIELDING;
        } else if (keyCode == KeyEvent.VK_SPACE && !isFiring) {
            playSound();
            isFiring = true;
            playerState = PlayerState.FIRING;

            projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
            projectileY = playerY;
            isProjectileVisible = true;

            new Thread(() -> {
                try {
                    Thread.sleep(500); // Limit firing rate
                    isFiring = false;
                    playerState = PlayerState.IDLE;
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        }
    }


    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        playerState = PlayerState.IDLE;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SpaceGame().setVisible(true);
            }
        });
    }
}