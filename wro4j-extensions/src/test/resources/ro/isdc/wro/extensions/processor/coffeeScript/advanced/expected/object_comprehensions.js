(function() {
  var age, ages, child, yearsOld;

  yearsOld = {
    max: 10,
    ida: 9,
    tim: 11
  };

  ages = (function() {
    var _results;

    _results = [];
    for (child in yearsOld) {
      age = yearsOld[child];
      _results.push(child + " is " + age);
    }
    return _results;
  })();

}).call(this);