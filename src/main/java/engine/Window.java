package engine;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
	private int width, height;
	private String title;
	private long glfwWindow, popUpWindow;
	private boolean hasDeadOpened, hasStaticOpened, hasDynamicOpened;

	private float r, g, b, a;
	private boolean isPaused;

	private static Window window = null;

	private static final int CELL_SIZE = 10;
	private boolean[][] cells;
	private boolean[][] nextGeneration;
	private boolean[][] prevGeneration;

	private long lastTime;
	private double fps;
	private int generation;

	private Window() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		this.width = (int) screenSize.getWidth();
		this.height = (int) screenSize.getHeight();
		this.title = "Game Of Life";
		this.isPaused = true;
		this.hasDeadOpened = false;
		this.hasDynamicOpened = false;
		this.hasStaticOpened = false;
		r = 0;
		b = 0;
		g = 0;
		a = 0;
		cells = new boolean[this.width / CELL_SIZE][this.height / CELL_SIZE];
		nextGeneration = new boolean[this.width / CELL_SIZE][this.height / CELL_SIZE];
		prevGeneration = new boolean[this.width / CELL_SIZE][this.height / CELL_SIZE];
		generation = 0;
	}

	public static Window get() {
		if (Window.window == null) {
			Window.window = new Window();
		}

		return Window.window;
	}

	public void run() {
		init();
		loop();

		// Free the memory
		glfwFreeCallbacks(glfwWindow);
		glfwDestroyWindow(glfwWindow);

		// Terminate GLFW and the free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	public void init() {
		// Setup an error callback
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW
		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW.");
		}

		// Configure Main window
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
		glfwWindowHint(GLFW_MAXIMIZED, GLFW_TRUE);

		// Create the Main window
		glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
		if (glfwWindow == NULL) {
			throw new IllegalStateException("Failed to create the Main window.");
		}

		glfwSetCursorPosCallback(glfwWindow, MouseListener::mousePosCallback);
		glfwSetMouseButtonCallback(glfwWindow, MouseListener::mouseButtonCallback);
		glfwSetScrollCallback(glfwWindow, MouseListener::mouseScrollCallback);
		glfwSetKeyCallback(glfwWindow, KeyListener::keyCallback);
		// Make the OpenGL context current
		glfwMakeContextCurrent(glfwWindow);
		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(glfwWindow);

		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();
	}

	private void clearGame() {
		for(int i=0; i<(this.width / CELL_SIZE); i++){
			Arrays.fill(cells[i], false);
			Arrays.fill(nextGeneration[i], false);
		}
		generation = 0;
		isPaused = true;
		hasStaticOpened = false;
		hasDynamicOpened = false;
		hasDeadOpened = false;
	}

	private void setRandGame() {
		int totalNumber = (this.height / CELL_SIZE) * (this.width / CELL_SIZE);
		double numberPerCent = ThreadLocalRandom.current().nextDouble(0.25, 0.85);
		double finalNumber = totalNumber * numberPerCent;
		for (int i = 0; i < (int) finalNumber; i++) {
			boolean isOk = false;
			do {
				int xPos = ThreadLocalRandom.current().nextInt(0, this.width) / CELL_SIZE;
				int yPos = ThreadLocalRandom.current().nextInt(0, this.height) / CELL_SIZE;
				if (!cells[xPos][yPos]) {
					cells[xPos][yPos] = true;
					isOk = true;
				}
			} while (!isOk);
		}
	}

	private void enableVSync() {
		glfwSwapInterval(1);
	}

	private void disableVSync() {
		glfwSwapInterval(0);
	}

	private void enablePause() {
		isPaused = true;
	}

	private void disablePause() {
		isPaused = false;
	}

	public void loop() {
		while (!glfwWindowShouldClose(glfwWindow)) {
			// Poll events
			glfwPollEvents();

			glClearColor(r, g, b, a);
			glClear(GL_COLOR_BUFFER_BIT);

			long now = System.nanoTime();

			if (KeyListener.isKeyPressed(GLFW_KEY_ESCAPE)) {
				break;
			}

			if (KeyListener.isKeyPressed(GLFW_KEY_O)) {
				disablePause();
			}

			if (KeyListener.isKeyPressed(GLFW_KEY_P)) {
				enablePause();
			}

			if (KeyListener.isKeyPressed(GLFW_KEY_C)) {
				clearGame();
			}

			if (KeyListener.isKeyPressed(GLFW_KEY_R)) {
				clearGame();
				setRandGame();
			}

			if (KeyListener.isKeyPressed(GLFW_KEY_V)) {
				enableVSync();
			}

			if (KeyListener.isKeyPressed(GLFW_KEY_D)) {
				disableVSync();
			}

			if (isPaused) {
				double xPos = MouseListener.getX() / CELL_SIZE;
				double yPos = MouseListener.getY() / CELL_SIZE;
				yPos = ((this.height / CELL_SIZE) - yPos);
				if (xPos >= 0 && xPos < (this.width / CELL_SIZE) && yPos >= 0 && yPos < (this.height / CELL_SIZE)) {
					if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_LEFT)) {
						cells[(int) xPos][(int) yPos] = false;
						this.hasDeadOpened = false;
						this.hasDynamicOpened = false;
						this.hasStaticOpened = false;
					}
					if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT)) {
						cells[(int) xPos][(int) yPos] = true;
						this.hasDeadOpened = false;
						this.hasDynamicOpened = false;
						this.hasStaticOpened = false;
					}
				}
				// Set up the projection matrix to be orthographic
				GL11.glMatrixMode(GL11.GL_PROJECTION);
				GL11.glLoadIdentity();
				GL11.glOrtho(0, width, 0, height, -1, 1);

				// Set up the modelview matrix to be the identity matrix
				GL11.glMatrixMode(GL11.GL_MODELVIEW);
				GL11.glLoadIdentity();

				// Set the color to white
				GL11.glColor3f(1.0f, 1.0f, 1.0f);

				// Draw the grid of squares
				for (int row = 0; row < cells.length; row++) {
					for (int col = 0; col < cells[row].length; col++) {
						int x = row * CELL_SIZE;
						int y = col * CELL_SIZE;
						if(cells[row][col]) {
							GL11.glBegin(GL11.GL_QUADS);
							GL11.glVertex2i(x, y);
							GL11.glVertex2i(x, y + CELL_SIZE);
							GL11.glVertex2i(x + CELL_SIZE, y + CELL_SIZE);
							GL11.glVertex2i(x + CELL_SIZE, y);
							GL11.glEnd();
						}
					}
				}
			}

			if (!isPaused) {
				update();
				GL11.glColor3f(1.0f, 1.0f, 1.0f);
				GL11.glBegin(GL11.GL_QUADS);
				for (int i = 0; i < cells.length; i++) {
					for (int j = 0; j < cells[i].length; j++) {
						if (cells[i][j]) {
							GL11.glVertex2f(i * CELL_SIZE, j * CELL_SIZE);
							GL11.glVertex2f((i + 1) * CELL_SIZE, j * CELL_SIZE);
							GL11.glVertex2f((i + 1) * CELL_SIZE, (j + 1) * CELL_SIZE);
							GL11.glVertex2f(i * CELL_SIZE, (j + 1) * CELL_SIZE);
						}
					}
				}
				GL11.glEnd();
			}

			double dt = (now - lastTime) / 1e9;
			fps = 1 / dt;
			lastTime = now;
			GLFW.glfwSetWindowTitle(glfwWindow, String.format("Game of Life - Generation: %d - FPS: %.1f", generation, fps));
			glfwSwapBuffers(glfwWindow);
		}
	}

	private int countNeighbors(int x, int y) {
		int count = 0;
		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				int nx = x + i;
				int ny = y + j;
				if (nx >= 0 && nx < cells.length && ny >= 0 && ny < cells[x].length) {
					if (i != 0 || j != 0) {
						if (cells[nx][ny]) {
							count++;
						}
					}
				}
			}
		}
		return count;
	}

	public boolean is2DArrayAllFalse(boolean[][] arr) {
		for (boolean[] row : arr) {
			for (boolean value : row) {
				if (value) {
					return false;
				}
			}
		}
		return true;
	}

	private void showPopUpWindow(String msg) {
		// Configure Pop-Up window
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

		// Create the Pop-Up window
		popUpWindow = glfwCreateWindow(500, 1, msg, NULL, NULL);
		if (popUpWindow == NULL) {
			throw new RuntimeException("Failed to create the Pop-Up window");
		}
		glfwSetCursorPosCallback(popUpWindow, MouseListener::mousePosCallback);
		glfwSetMouseButtonCallback(popUpWindow, MouseListener::mouseButtonCallback);
		glfwSetScrollCallback(popUpWindow, MouseListener::mouseScrollCallback);
		glfwSetKeyCallback(popUpWindow, KeyListener::keyCallback);
		// Make the OpenGL context current
		glfwMakeContextCurrent(popUpWindow);
		// Enable v-sync
		glfwSwapInterval(1);

		// Make the window visible
		glfwShowWindow(popUpWindow);

		while (!glfwWindowShouldClose(popUpWindow)) {
			glfwPollEvents();
			/*if (KeyListener.isKeyPressed(GLFW_KEY_ESCAPE)) {
				break;
			}*/
			glfwSwapBuffers(popUpWindow);
		}
		// Destroy the pop-up window
		GLFW.glfwDestroyWindow(popUpWindow);
		// Make the OpenGL context current
		glfwMakeContextCurrent(glfwWindow);
	}

	private void update() {
		if (is2DArrayAllFalse(cells)){
			if (!(hasDeadOpened || hasDynamicOpened || hasStaticOpened)){
				showPopUpWindow("Configuration of all-dead cells");
				hasDeadOpened = true;
			}
			return;
		}

		generation++;

		// Copy current state of cells to temporary array
		for (int i = 0; i < cells.length; i++) {
			System.arraycopy(cells[i], 0, nextGeneration[i], 0, cells[i].length);
			if(generation % 2 == 1) System.arraycopy(cells[i], 0, prevGeneration[i], 0, cells[i].length);
		}

		// Update state of each cell based on its neighbors
		for (int i = 0; i < cells.length; i++) {
			for (int j = 0; j < cells[i].length; j++) {
				int numNeighbors = countNeighbors(i, j);
				if (cells[i][j]) {
					// Any live cell with fewer than two live neighbors dies, as if by underpopulation.
					// Any live cell with more than three live neighbors dies, as if by overpopulation.
					if (numNeighbors < 2) {
						nextGeneration[i][j] = false;
					}
					// Any live cell with two or three live neighbors lives on to the next generation.
					else nextGeneration[i][j] = numNeighbors == 2 || numNeighbors == 3;
				} else {
					// Any dead cell with exactly three live neighbors becomes a live cell, as if by reproduction.
					if (numNeighbors == 3) {
						nextGeneration[i][j] = true;
					}
				}
			}
		}

		if (Arrays.deepEquals(cells, nextGeneration) && !(hasDeadOpened || hasDynamicOpened || hasStaticOpened)) {
			showPopUpWindow("Stable and static cell configuration");
			hasStaticOpened = true;
		} else if (Arrays.deepEquals(prevGeneration, nextGeneration) && !(hasDeadOpened || hasDynamicOpened || hasStaticOpened)) {
			showPopUpWindow("Stable but not static cell configuration"); // It only check if the previous configuration is the same as the next one
			hasDynamicOpened = true;
		}

		// Update current state of cells to temporary array
		for (int i = 0; i < cells.length; i++) {
			System.arraycopy(nextGeneration[i], 0, cells[i], 0, nextGeneration[i].length);
		}
	}
}