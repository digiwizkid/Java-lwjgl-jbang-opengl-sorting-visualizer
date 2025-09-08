//usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS org.lwjgl:lwjgl:3.3.3
//DEPS org.lwjgl:lwjgl-glfw:3.3.3
//DEPS org.lwjgl:lwjgl-opengl:3.3.3
//DEPS org.lwjgl:lwjgl:3.3.3:natives-linux
//DEPS org.lwjgl:lwjgl:3.3.3:natives-macos
//DEPS org.lwjgl:lwjgl:3.3.3:natives-windows
//DEPS org.lwjgl:lwjgl-glfw:3.3.3:natives-linux
//DEPS org.lwjgl:lwjgl-glfw:3.3.3:natives-macos
//DEPS org.lwjgl:lwjgl-glfw:3.3.3:natives-windows
//DEPS org.lwjgl:lwjgl-opengl:3.3.3:natives-linux
//DEPS org.lwjgl:lwjgl-opengl:3.3.3:natives-macos
//DEPS org.lwjgl:lwjgl-opengl:3.3.3:natives-windows

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.Random;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class BubbleSortVisualizer {
    
    private long window;
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int ARRAY_SIZE = 20;
    
    private int[] array = {45, 23, 78, 12, 89, 34, 67, 56, 90, 15, 
                          38, 72, 29, 84, 41, 63, 17, 95, 26, 58};
    private int currentI = -1;
    private int currentJ = -1;
    private boolean sorting = false;
    private boolean sortComplete = false;
    private long lastUpdate = 0;
    private static final long DELAY_MS = 200; // Delay between sorting steps

    public void run() {
        init();
        loop();
        
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
    
    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "Bubble Sort Visualization", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
            if (key == GLFW_KEY_SPACE && action == GLFW_RELEASE && !sorting && !sortComplete) {
                sorting = true;
                currentI = 0;
                currentJ = 0;
            }
            if (key == GLFW_KEY_R && action == GLFW_RELEASE) {
                resetArray();
            }
        });
        
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            glfwSetWindowPos(window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
    }
    
    private void loop() {
        GL.createCapabilities();
        glClearColor(0.1f, 0.1f, 0.1f, 0.0f);
        
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            // Update sorting
            if (sorting && !sortComplete) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastUpdate > DELAY_MS) {
                    bubbleSortStep();
                    lastUpdate = currentTime;
                }
            }
            
            // Draw array
            drawArray();
            
            // Draw instructions
            drawText();
            
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
    
    private void bubbleSortStep() {
        if (currentI < ARRAY_SIZE - 1) {
            if (currentJ < ARRAY_SIZE - currentI - 1) {
                if (array[currentJ] > array[currentJ + 1]) {
                    // Swap elements
                    int temp = array[currentJ];
                    array[currentJ] = array[currentJ + 1];
                    array[currentJ + 1] = temp;
                }
                currentJ++;
            } else {
                currentI++;
                currentJ = 0;
            }
        } else {
            sorting = false;
            sortComplete = true;
            currentI = -1;
            currentJ = -1;
        }
    }
    
    private void drawArray() {
        int maxValue = 100; // Maximum value for scaling
        float barWidth = (float) WINDOW_WIDTH / ARRAY_SIZE;
        
        for (int i = 0; i < ARRAY_SIZE; i++) {
            float barHeight = ((float) array[i] / maxValue) * (WINDOW_HEIGHT - 100);
            float x = i * barWidth;
            float y = 50;
            
            // Set color based on current operation
            if (sorting && (i == currentJ || i == currentJ + 1)) {
                glColor3f(1.0f, 0.3f, 0.3f); // Red for elements being compared
            } else if (sortComplete) {
                glColor3f(0.3f, 1.0f, 0.3f); // Green when sorted
            } else {
                glColor3f(0.7f, 0.7f, 1.0f); // Blue for normal elements
            }
            
            // Draw bar
            glBegin(GL_QUADS);
            glVertex2f(x + 2, y);
            glVertex2f(x + barWidth - 2, y);
            glVertex2f(x + barWidth - 2, y + barHeight);
            glVertex2f(x + 2, y + barHeight);
            glEnd();
        }
        
        // Set up 2D orthographic projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, WINDOW_WIDTH, 0, WINDOW_HEIGHT, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }
    
    private void drawText() {
        // Simple text indication (since LWJGL text rendering is complex)
        glColor3f(1.0f, 1.0f, 1.0f);
        
        // Draw status indicator with rectangles
        if (!sorting && !sortComplete) {
            // Draw "Press SPACE to start" indicator
            glBegin(GL_QUADS);
            glVertex2f(10, WINDOW_HEIGHT - 30);
            glVertex2f(200, WINDOW_HEIGHT - 30);
            glVertex2f(200, WINDOW_HEIGHT - 10);
            glVertex2f(10, WINDOW_HEIGHT - 10);
            glEnd();
        } else if (sorting) {
            // Draw "Sorting..." indicator
            glBegin(GL_QUADS);
            glVertex2f(10, WINDOW_HEIGHT - 30);
            glVertex2f(100, WINDOW_HEIGHT - 30);
            glVertex2f(100, WINDOW_HEIGHT - 10);
            glVertex2f(10, WINDOW_HEIGHT - 10);
            glEnd();
        } else if (sortComplete) {
            // Draw "Sorted!" indicator
            glBegin(GL_QUADS);
            glVertex2f(10, WINDOW_HEIGHT - 30);
            glVertex2f(80, WINDOW_HEIGHT - 30);
            glVertex2f(80, WINDOW_HEIGHT - 10);
            glVertex2f(10, WINDOW_HEIGHT - 10);
            glEnd();
        }
    }
    
    private void resetArray() {
        // Shuffle the array
        Random random = new Random();
        for (int i = 0; i < ARRAY_SIZE; i++) {
            int randomIndex = random.nextInt(ARRAY_SIZE);
            int temp = array[i];
            array[i] = array[randomIndex];
            array[randomIndex] = temp;
        }
        
        sorting = false;
        sortComplete = false;
        currentI = -1;
        currentJ = -1;
    }
    
    public static void main(String[] args) {
        new BubbleSortVisualizer().run();
    }
}

/*
 * jbang BubbleSortVisualizer.java
 * SPACE to start sorting
 * R to reset/shuffle
 * ESC to exit
 * 
 */