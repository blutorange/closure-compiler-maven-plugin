/** @const */
var DEBUG = false;

function logMe() {
    if (DEBUG) {
        console.log('remove me');
    }
}

/** @const */
const user = {
    /** @const */
    name: "John",
    /** @const */
    age: 30,
    isAdmin: false
};


function foo() {
    var i = 42;
    console.log("foo", i);
    console.log("bar", i);
    return 2;
}

console.log("hello", user.name, user.age, user.isAdmin);
console.log(foo());

function props() {
    var x = {prop1: 1, prop2: 2};
    alert(x.prop2);
}

class TestClass {
    constructor() {
        /** @const  {string!} */
        this.styles = "bar";
    }

    dumpStyles() {
        console.log(this.styles);

        const inScopeStyles = [ '1', '2', '3' ];
        console.log(inScopeStyles.join(';'));
    }
}

new TestClass().dumpStyles();
