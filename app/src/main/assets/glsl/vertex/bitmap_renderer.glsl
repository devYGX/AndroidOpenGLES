attribute vec4 vPosition;
attribute vec2 vTexPosition;
uniform mat4 mMatrix;
varying vec2 vvTexPosition;


void main() {
    gl_Position = mMatrix * vPosition;
    vvTexPosition = vTexPosition;
}