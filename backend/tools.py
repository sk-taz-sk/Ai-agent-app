def calculate(expression):
    try:
        return eval(expression, {"__builtins__": {}}, {})
    except:
        return "Invalid calculation"
