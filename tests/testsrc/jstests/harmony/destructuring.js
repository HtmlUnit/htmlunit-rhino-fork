load("testsrc/assert.js");

// This is not destructuring.
// However, there was a bug as a side effect of destructuring, so write the test here.
// If there is another suitable place, please move it.
assertDoesNotThrow("for ({}; false;){}");
assertDoesNotThrow("for ([]; false;){}");

var a = { b: 123 };
var c;
for ({ b: c } = a; false; );
assertEquals(c, 123);

var d = [234];
var e;
for ([e] = d; false; );
assertEquals(e, 234);

var arr = [];
[arr[+false + 1]] = [123];
assertEquals(arr[1], 123);

[(NaN, arr)[1]] = [234];
assertEquals(arr[1], 234);

var obj = {};
[(NaN, obj).b] = [345];
assertEquals(obj.b, 345);

assertThrows("(1 ? {} : 490) = 1", SyntaxError);
assertThrows("(1 ? [] : 490) = 1", SyntaxError);

"success";
