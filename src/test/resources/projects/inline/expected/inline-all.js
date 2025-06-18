'use strict';
const user = {name:"John", age:30, isAdmin:!1};
console.log("hello", user.name, user.age, user.isAdmin);
var JSCompiler_temp_const$jscomp$1 = console, JSCompiler_temp_const$jscomp$0 = JSCompiler_temp_const$jscomp$1.log;
console.log("foo", 42);
console.log("bar", 42);
JSCompiler_temp_const$jscomp$0.call(JSCompiler_temp_const$jscomp$1, 2);
class TestClass {
  constructor() {
    this.styles = "bar";
  }
  dumpStyles() {
    console.log("bar");
    console.log("1;2;3");
  }
}
(new TestClass()).dumpStyles();

