//#version 120
attribute vec4 vPosition;
attribute vec2 vTexturePosition;
uniform mat4 mMatrix;
uniform mat4 mTexMatrix;

varying vec2 mvTexturePosition;

void main() {
    gl_Position = mMatrix * vPosition;
    mvTexturePosition = (mTexMatrix * vec4(vTexturePosition, 0.0, 1.0)).xy;
}