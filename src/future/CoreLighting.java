/*
 * Copyright (c) 2012, Oskar Veerhoek
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the FreeBSD Project.
 */

package future;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import utility.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

/**
 * NOT DONE YET
 * Shows lighting without using the fixed function pipeline.
 */
public class CoreLighting {

    private static EulerCamera cam;
    private static int shaderProgram;
    private static int vboVertexHandle;
    private static int vboNormalHandle;
    private static int uniformAmbientLight;
    private static int uniformNormalMatrix;
    private static int uniformModelViewMatrix;
    private static int uniformModelViewProjectionMatrix;
    private static int uniformLightPosition;
    private static int uniformShininess;
    private static int attributeVertex;
    private static int attributeColour;
    private static int attributeNormal;

    private static Model model;

    public static final String MODEL_LOCATION = "res/models/bunny.obj";
    public static final String VERTEX_SHADER_LOCATION = "res/shaders/fragment_phong_lighting_core.vs";
    public static final String FRAGMENT_SHADER_LOCATION = "res/shaders/fragment_phong_lighting_core.fs";

    public static void main(String[] args) {
        setUpDisplay();
        setUpVBOs();
        setUpCamera();
        setUpShaders();
        setUpLighting();
        while (!Display.isCloseRequested()) {
            render();
            checkInput();
            Display.update();
            Display.sync(60);
        }
        cleanUp();
        System.exit(0);
    }

    private static void checkInput() {
        cam.processMouse(1, 80, -80);
        cam.processKeyboard(16, 1, 1, 1);
        if (Mouse.isButtonDown(0))
            Mouse.setGrabbed(true);
        else if (Mouse.isButtonDown(1))
            Mouse.setGrabbed(false);
    }

    private static void cleanUp() {
        glDeleteProgram(shaderProgram);
        glDeleteBuffers(vboVertexHandle);
        glDeleteBuffers(vboNormalHandle);
        Display.destroy();
    }

    private static void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glLoadIdentity();
        cam.applyTranslations();
        glUseProgram(shaderProgram);
        glLight(GL_LIGHT0, GL_POSITION, asFloatBuffer(cam.getX(), cam.getY(), cam.getZ(), 1));
        glBindBuffer(GL_ARRAY_BUFFER, vboVertexHandle);
        glVertexPointer(3, GL_FLOAT, 0, 0L);
        glBindBuffer(GL_ARRAY_BUFFER, vboNormalHandle);
        glNormalPointer(GL_FLOAT, 0, 0L);
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_NORMAL_ARRAY);
        glColor3f(0.4f, 0.27f, 0.17f);
        glMaterialf(GL_FRONT, GL_SHININESS, 10f);
        glDrawArrays(GL_TRIANGLES, 0, model.faces.size() * 3);
        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState(GL_NORMAL_ARRAY);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glUseProgram(0);
    }

    private static void setUpLighting() {
        glShadeModel(GL_SMOOTH);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_LIGHTING);
        glEnable(GL_LIGHT0);
        glLightModel(GL_LIGHT_MODEL_AMBIENT, asFloatBuffer(new float[]{0.05f,
                0.05f, 0.05f, 1f}));
        glLight(GL_LIGHT0, GL_POSITION,
                asFloatBuffer(new float[]{0, 0, 0, 1}));
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_DIFFUSE);
    }

    private static void setUpVBOs() {
        int[] vbos;
        try {
            model = OBJLoader.loadModel(new File(MODEL_LOCATION));
            int vboVertexHandle = glGenBuffers();
            int vboNormalHandle = glGenBuffers();
            FloatBuffer vertices = BufferTools.reserveData(model.faces.size() * 9);
            FloatBuffer normals = BufferTools.reserveData(model.faces.size() * 9);
            for (Face face : model.faces) {
                vertices.put(BufferTools.asFloats(model.vertices.get((int) face.vertex.x - 1)));
                vertices.put(BufferTools.asFloats(model.vertices.get((int) face.vertex.y - 1)));
                vertices.put(BufferTools.asFloats(model.vertices.get((int) face.vertex.z - 1)));
                normals.put(BufferTools.asFloats(model.normals.get((int) face.normal.x - 1)));
                normals.put(BufferTools.asFloats(model.normals.get((int) face.normal.y - 1)));
                normals.put(BufferTools.asFloats(model.normals.get((int) face.normal.z - 1)));
            }
            vertices.flip();
            normals.flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboVertexHandle);
            glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
            glEnableVertexAttribArray(attributeVertex);
            glVertexAttribPointer(attributeVertex, 3, false, 0, vertices);
            glBindBuffer(GL_ARRAY_BUFFER, vboNormalHandle);
            glEnableVertexAttribArray(attributeNormal);
            glBufferData(GL_ARRAY_BUFFER, normals, GL_STATIC_DRAW);
            glNormalPointer(GL_FLOAT, 0, 0L);
            // TODO: This really isn't finished yet. :-(
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            cleanUp();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            cleanUp();
            System.exit(1);
        }
    }

    private static void setUpShaders() {
        shaderProgram = ShaderLoader.loadShaderPair(VERTEX_SHADER_LOCATION, FRAGMENT_SHADER_LOCATION);
        attributeVertex = glGetAttribLocation(shaderProgram, "attributeVertex");
        attributeColour = glGetAttribLocation(shaderProgram, "attributeColour");
        attributeNormal = glGetAttribLocation(shaderProgram, "attributeNormal");
        uniformAmbientLight = glGetUniformLocation(shaderProgram, "uniformAmbientLight");
        uniformLightPosition = glGetUniformLocation(shaderProgram, "uniformLightPosition");
        uniformModelViewMatrix = glGetUniformLocation(shaderProgram, "uniformModelViewMatrix");
        uniformModelViewProjectionMatrix = glGetUniformLocation(shaderProgram, "uniformModelViewProjectionMatrix");
        uniformNormalMatrix = glGetUniformLocation(shaderProgram, "uniformNormalMatrix");
        uniformShininess = glGetUniformLocation(shaderProgram, "uniformShininess");
        glValidateProgram(shaderProgram);
    }

    private static void setUpCamera() {
        cam = new EulerCamera((float) Display.getWidth()
                / (float) Display.getHeight(), -2.19f, 1.36f, 11.45f);
        cam.setFov(70);
        cam.applyPerspectiveMatrix();
    }

    private static void setUpDisplay() {
        try {
            Display.setDisplayMode(new DisplayMode(640, 480));
            Display.setVSyncEnabled(true);
            Display.setTitle("Core Lighting Demo");
            Display.create();
        } catch (LWJGLException e) {
            System.err.println("The display wasn't initialized correctly. :(");
            Display.destroy();
            System.exit(1);
        }
    }

    private static FloatBuffer asFloatBuffer(float... values) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(values.length);
        buffer.put(values);
        buffer.flip();
        return buffer;
    }
}
