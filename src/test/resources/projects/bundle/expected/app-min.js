'use strict';class Vector2{constructor(a,b){this.x=a;this.y=b}add(a){return new Vector2(this.x+a.x,this.y+a.y)}};function main(){var a=new Vector2(1,2),b=new Vector2(3,4);console.log(a.add(b))};
