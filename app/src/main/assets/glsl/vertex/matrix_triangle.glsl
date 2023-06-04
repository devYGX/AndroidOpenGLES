attribute vec4 vPosition;
uniform mat4 mMatrix;

void main() {
    gl_Position = mMatrix * vPosition;
}