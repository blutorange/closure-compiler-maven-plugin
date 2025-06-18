'use strict';
var DEBUG = !1;
const user = {name:"John", age:30, isAdmin:!1};
console.log("hello", user.name, user.age, user.isAdmin);
var JSCompiler_temp_const$jscomp$1 = console, JSCompiler_temp_const$jscomp$0 = JSCompiler_temp_const$jscomp$1.log, JSCompiler_inline_result$jscomp$2, i$jscomp$inline_3 = 42;
console.log("foo", i$jscomp$inline_3);
console.log("bar", i$jscomp$inline_3);
JSCompiler_inline_result$jscomp$2 = 2;
JSCompiler_temp_const$jscomp$0.call(JSCompiler_temp_const$jscomp$1, JSCompiler_inline_result$jscomp$2);
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

