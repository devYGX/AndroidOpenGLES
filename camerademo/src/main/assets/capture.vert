//#version 120
attribute vec4 vPosition;
attribute vec2 vTexturePosition;
uniform mat4 mMatrix;

varying vec2 mvTexturePosition;

void main() {
    gl_Position = mMatrix * vPosition;
    mvTexturePosition = vTexturePosition;
}