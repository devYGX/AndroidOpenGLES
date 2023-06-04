precision mediump float;
uniform sampler2D sSampler;

varying vec2 vvTexPosition;

void main() {
    gl_FragColor = texture2D(sSampler, vvTexPosition);
}