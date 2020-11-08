const STATUS = {FAIL : 'F', PASS : 'P', OBJECT_MISMATCH : 'OM', NOT_EXISTS:'NE'};
const NEGATIVE_INFINITY = -999999999;
const POSITIVE_INFINITY = 999999999;
const POSS
function compare (expected, actual) {
    var result = {status: STATUS.PASS};

    //TODO: Handle all ! condition. FOr example boolean, 0, null, undefined, etc.
    if(expected===null && actual === null
        || expected === undefined && actual === undefined
        || (expected!==0) !expected && !actual
        || !expected
        || !actual) {
        return failedResult(result, expected, actual);
    } else if (typeof actual !== 'object' && typeof expected !== 'object') {
        //TODO: Handle Primitive types.
        if(expected !== actual) {
            return failedResult(result, expected, actual);
        } else {
            return result;
        }
    }

    if(actual.constructor === Object && expected.constructor === Array
        || actual.constructor === Array && expected.constructor === Object) {
        return failedResult(result, expected, actual, STATUS.OBJECT_MISMATCH);
    }

    if(actual.constructor === Array) {
        return compareArray(expected, actual);
    }

    return compareObject(expected, actual);
}

//TODO: put proper null blank, 0, undefined check
function compareArray(expected, actual) {
    var matStatus = {status: STATUS.PASS};
    if(!expected && !actual) {
        return finalMatStatus;
    } else if(expected && !actual || !expected && actual) {
        return failedResult(finalMatStatus, expected, actual);
    } else if (expected.length === 0 && actual.length === 0)) {
        return finalMatStatus;
    }

    var crossResults = [];
    for(var i=0; i<expected.length; i++) {
        crossResults.push(findBestMatching(expected[i], i, actual[i]));
    }

    return calculateBestMatchingAndMatch(expected, actual, crossResults);
}

function compareObject(expected, actual) {
    var finalMatStatus = {status: STATUS.PASS};
    if(!expected && !actual) {
        return finalMatStatus;
    } else if(expected && !actual || !expected && actual) {
        return failedResult(finalMatStatus, expected, actual);
    } else if (Object.keys(expected).length === 0 && Object.keys(actual.length) === 0)) {
        return finalMatStatus;
    }

    var expKeys = Object.keys(expected);
    var actKeys = Object.keys(actual);
    var i = 0;
    finalMatStatus.diff = {};
    var matCount = 0;
    for(; i<expKeys.length; i++) {
        var internalMatStatus = {status: STATUS.PASS};
        var expVal = expected[expKeys[i]];
        var actVal = actual[expKeys[i]];
        var matStatus =  compare(expVal, actVal);
        matCount = matStatus.status === STATUS.PASS ? matCount+1 : matCount;
        if(matStatus.status === STATUS.FAIL) {
            failedResult(internalMatStatus, expVal, actVal);
            internalMatStatus.diff = matStatus.diff;
            finalMatStatus.status = STATUS.FAIL;
            finalMatStatus.diff[expKeys[i]] = internalMatStatus;
        }
    }

    return finalMatStatus;
}

function calculateBestMatchingAndMatch(expected, actual, results) {
    var matrix = [];

    for(var i=0; i<expected.length; i++){
        matrix[i] = [];
        for(var j=0; i<expected.length; j++){
            matrix[i][j] = false;
        }
    }

    var blockMatrix = function(index) {
        for(car i=0; i<matrix.length; i++) {
            matrix[i][index] = true;
        }
    };

    var diffObj = {};
    var nonMatching = [];
    var finalStatus = true;
    for(var i=0; i<results.length; i++) {
        var matchingObj = results[i].filter(res => res.status === STATUS.PASS);
        var matching == !!matchingObj.length;
        finalStatus = finalStatus && matching;

        if(matching) {
            blockMatrix(matrix, matchingObj[0].matchingIndex);
            diffObj[results[i][0].elementIndex] = matchingObj[0];
        } else {
            nonMatching.push(results[i]);
        }
    }

    nonMatching.sort(function(a,b) {
        return a.matchingCount && b.matchingCount? a.matchingCount - b.matchingCount : 0;
    });

    var findBestAndBlockMatrix = function(items) {
        var matches = items.filter(item => item.elementIndex && item.matchingIndex && !matrix[item.elementIndex][item.matchingIndex]);
        if(matches.length) {
            blockMatrix(matches[0]);
            return matches[0];
        } else {
            return {status: STATUS.NOT_EXISTS};
        }
    };

    for(var i=0; i<nonMatching.length; i++) {
        diffObj[nonMatching[i][0].elementIndex] = findBestAndBlockMatrix(nonMatching[i]);
    }

    var finalMatStatus = {status: finalStatus ? STATUS.PASS: STATUS.FAIL};

    if(!finalStatus) {
        failedResult(finalMatStatus, expected, actual);
        finalMatStatus.diff = diffObj;
    }

    return finalMatStatus;
}

function findBestMatching(obj, elemIndex, array) {
    var crossResults = [];
    if(!obj || !array) {
        return crossResults;
    } else if(array.length === 0){
        crossResults.push({status: STATUS.FAIL, exp: obj, elementIndex: elemIndex});
        return crossResults;
    }

    for(int i=0; i<array.length; i++) {
        var result = {status: STATUS.FAIL, matchingCount: 0, matchingIndex: i, elementIndex: elemIndex};
        if (typeof obj !== 'object' && typeof array[i] !== 'object') {
            if(expected !== actual) {
                result.status = STATUS.PASS;
                result.matchingCount = POSITIVE_INFINITY;
            }
        } else if (obj.constructor === Object && array[i].constructor === Object) {
            result = compareObject(obj, array[i]);
        }

        //TODO: Fix [[],[],[]...]
        if(result.status === STATUS.FAIL) {
            failedResult(result, obj, array[i], STATUS.FAIL);
            result.matchingIndex = i;
        }
        crossResults.push(result);
    }

    return crossResults;
}

function failedResult(result, expected, actual, status = STATUS.FAIL) {
    result.exp = expected;
    result.act = actual;
    result.status = status;
    return result;
}