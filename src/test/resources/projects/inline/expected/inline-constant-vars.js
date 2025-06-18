'use strict';
function logMe() {
}
const user = {name:"John", age:30, isAdmin:!1};
function foo() {
  console.log("foo", 42);
  console.log("bar", 42);
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

