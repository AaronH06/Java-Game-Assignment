import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class Tetris extends JFrame {
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private final int TILE_SIZE = 30;
    private final Color[] colors = {
            Color.BLACK, Color.CYAN, Color.BLUE, Color.ORANGE,
            Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.RED
    };
    private final int[][][] SHAPES = {
            {{1, 1, 1, 1}}, // I
            {{2, 0, 0}, {2, 2, 2}}, // J
            {{0, 0, 3}, {3, 3, 3}}, // L
            {{4, 4}, {4, 4}}, // O
            {{0, 5, 5}, {5, 5, 0}}, // S
            {{0, 6, 0}, {6, 6, 6}}, // T
            {{7, 7, 0}, {0, 7, 7}}  // Z
    };

    private int[][] board = new int[BOARD_HEIGHT][BOARD_WIDTH];
    private int currentX = 0, currentY = 0;
    private int[][] currentPiece;
    private int[][] heldPiece = null; // Piece held by the player
    private boolean canHold = true;   // To allow holding only once per drop
    private Color currentColor;
    private boolean isGameOver = false;

    private int score = 0;
    private long startTime;

    private JPanel gamePanel;
    private JLabel timerLabel, scoreLabel, holdBoxLabel;

    public Tetris() {
        setTitle("Tetris");
        setSize(500, BOARD_HEIGHT * TILE_SIZE + 50);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // Main Layout
        setLayout(new BorderLayout());

        // Game Panel (Center)
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawGame(g);
            }
        };
        gamePanel.setPreferredSize(new Dimension(BOARD_WIDTH * TILE_SIZE, BOARD_HEIGHT * TILE_SIZE));
        add(gamePanel, BorderLayout.CENTER);

        // Info Panel (Right)
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setPreferredSize(new Dimension(150, BOARD_HEIGHT * TILE_SIZE));
        infoPanel.setBackground(Color.DARK_GRAY);

        timerLabel = new JLabel("Time: 0s", SwingConstants.CENTER);
        timerLabel.setForeground(Color.WHITE);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        scoreLabel = new JLabel("Score: 0", SwingConstants.CENTER);
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        holdBoxLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawHoldBox(g);
            }
        };
        holdBoxLabel.setPreferredSize(new Dimension(120, 120));
        holdBoxLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        holdBoxLabel.setBackground(Color.BLACK);
        holdBoxLabel.setOpaque(true);

        infoPanel.add(Box.createVerticalStrut(20));
        infoPanel.add(timerLabel);
        infoPanel.add(Box.createVerticalStrut(40));
        infoPanel.add(scoreLabel);
        infoPanel.add(Box.createVerticalStrut(40));
        infoPanel.add(holdBoxLabel);
        add(infoPanel, BorderLayout.EAST);

        // Key Listener
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (isGameOver) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        resetGame();
                    }
                    return;
                }

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> movePiece(-1, 0);
                    case KeyEvent.VK_RIGHT -> movePiece(1, 0);
                    case KeyEvent.VK_DOWN -> movePiece(0, 1);
                    case KeyEvent.VK_UP -> rotatePiece();
                    case KeyEvent.VK_SPACE -> dropPiece();
                    case KeyEvent.VK_C -> holdPiece(); // Hold the current piece
                }
                gamePanel.repaint();
                holdBoxLabel.repaint();
            }
        });

        Timer timer = new Timer(500, e -> {
            if (!isGameOver) {
                if (!movePiece(0, 1)) {
                    mergePiece();
                    clearLines();
                    spawnPiece();
                }
                gamePanel.repaint();
            }
        });
        timer.start();

        Timer timeUpdater = new Timer(1000, e -> {
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            timerLabel.setText("Time: " + elapsedSeconds + "s");
        });
        timeUpdater.start();

        startTime = System.currentTimeMillis();
        spawnPiece();
    }

    private void rotatePiece() {
        int[][] rotatedPiece = new int[currentPiece[0].length][currentPiece.length];
        for (int i = 0; i < currentPiece.length; i++) {
            for (int j = 0; j < currentPiece[i].length; j++) {
                rotatedPiece[j][currentPiece.length - 1 - i] = currentPiece[i][j];
            }
        }
        if (canPlacePiece(currentX, currentY, rotatedPiece)) {
            currentPiece = rotatedPiece;
        }
    }

    private boolean canPlacePiece(int x, int y, int[][] piece) {
        for (int i = 0; i < piece.length; i++) {
            for (int j = 0; j < piece[i].length; j++) {
                if (piece[i][j] != 0) {
                    int newX = x + j;
                    int newY = y + i;
                    if (newX < 0 || newX >= BOARD_WIDTH || newY >= BOARD_HEIGHT || board[newY][newX] != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void dropPiece() {
        while (movePiece(0, 1)) {
            // Move the piece down until it can't move anymore
        }
        mergePiece();
        clearLines();
        spawnPiece();
    }

    private void holdPiece() {
        if (!canHold) return;

        if (heldPiece == null) {
            heldPiece = currentPiece;
            spawnPiece();
        } else {
            int[][] temp = heldPiece;
            heldPiece = currentPiece;
            currentPiece = temp;
            currentX = BOARD_WIDTH / 2 - currentPiece[0].length / 2;
            currentY = 0;
        }
        canHold = false;
    }

    private void spawnPiece() {
        Random rand = new Random();
        int shapeIndex = rand.nextInt(SHAPES.length);
        currentPiece = SHAPES[shapeIndex];
        currentColor = colors[shapeIndex + 1];
        currentX = BOARD_WIDTH / 2 - currentPiece[0].length / 2;
        currentY = 0;

        if (!canPlacePiece(currentX, currentY, currentPiece)) {
            isGameOver = true;
            gamePanel.repaint();
        }

        canHold = true; // Reset hold ability
    }

    private boolean movePiece(int dx, int dy) {
        if (canPlacePiece(currentX + dx, currentY + dy, currentPiece)) {
            currentX += dx;
            currentY += dy;
            return true;
        }
        return false;
    }

    private void mergePiece() {
        for (int i = 0; i < currentPiece.length; i++) {
            for (int j = 0; j < currentPiece[i].length; j++) {
                if (currentPiece[i][j] != 0) {
                    board[currentY + i][currentX + j] = currentPiece[i][j];
                }
            }
        }
    }

    private void clearLines() {
        int linesCleared = 0;
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            boolean isFull = true;
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (board[i][j] == 0) {
                    isFull = false;
                    break;
                }
            }
            if (isFull) {
                for (int k = i; k > 0; k--) {
                    System.arraycopy(board[k - 1], 0, board[k], 0, BOARD_WIDTH);
                }
                for (int j = 0; j < BOARD_WIDTH; j++) {
                    board[0][j] = 0;
                }
                linesCleared++;
            }
        }
        score += linesCleared * 100;
        scoreLabel.setText("Score: " + score);
    }

    private void drawGame(Graphics g) {
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                g.setColor(colors[board[i][j]]);
                g.fillRect(j * TILE_SIZE, i * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                g.setColor(Color.BLACK);
                g.drawRect(j * TILE_SIZE, i * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        if (isGameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 15));
            g.drawString("Game Over! Press Space to Restart", 50, BOARD_HEIGHT * TILE_SIZE / 2);
        } else {
            for (int i = 0; i < currentPiece.length; i++) {
                for (int j = 0; j < currentPiece[i].length; j++) {
                    if (currentPiece[i][j] != 0) {
                        g.setColor(currentColor);
                        g.fillRect((currentX + j) * TILE_SIZE, (currentY + i) * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        g.setColor(Color.BLACK);
                        g.drawRect((currentX + j) * TILE_SIZE, (currentY + i) * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }
    }

    private void drawHoldBox(Graphics g) {
        if (heldPiece != null) {
            for (int i = 0; i < heldPiece.length; i++) {
                for (int j = 0; j < heldPiece[i].length; j++) {
                    if (heldPiece[i][j] != 0) {
                        g.setColor(colors[heldPiece[i][j]]);
                        g.fillRect(j * TILE_SIZE, i * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        g.setColor(Color.BLACK);
                        g.drawRect(j * TILE_SIZE, i * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
        }
    }

    private void resetGame() {
        board = new int[BOARD_HEIGHT][BOARD_WIDTH];
        score = 0;
        scoreLabel.setText("Score: 0");
        isGameOver = false;
        spawnPiece();
        gamePanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Tetris game = new Tetris();
            game.setVisible(true);
        });
    }
}