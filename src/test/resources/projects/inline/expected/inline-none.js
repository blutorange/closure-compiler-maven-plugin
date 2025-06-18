'use strict';
var DEBUG = !1;
function logMe() {
  DEBUG && console.log("remove me");
}
const user = {name:"John", age:30, isAdmin:!1};
function foo() {
  var a = 42;
  console.log("foo", a);
  console.log("bar", a);
  return 2;
}
console.log("hello", user.name, user.age, user.isAdmin);
console.log(foo());
function props() {
  var a = 2;
  alert(a);
}
class TestClass {
  constructor() {
    this.styles = "bar";
  }
  dumpStyles() {
    console.log(this.styles);
    const a = ["1", "2", "3"];
    console.log(a.join(";"));
  }
}
(new TestClass()).dumpStyles();

