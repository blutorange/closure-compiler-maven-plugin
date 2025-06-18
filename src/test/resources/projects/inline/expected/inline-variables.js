'use strict';
function logMe() {
}
const user = {name:"John", age:30, isAdmin:!1};
console.log("hello", user.name, user.age, user.isAdmin);
console.log(function() {
  console.log("foo", 42);
  console.log("bar", 42);
  return 2;
}());
function props() {
  alert(2);
}
class TestClass {
  constructor() {
    this.styles = "bar";
  }
  dumpStyles() {
    console.log(this.styles);
    console.log("1;2;3");
  }
}
(new TestClass()).dumpStyles();

