object Main {
  def main(arguments: Array[String]) = {
    val just_product = product(x => x)(_,_)
    println(just_product(2,5)) // 120
    // println(factorial(3)) //6

    val general_product = general(x => x)(_:Int,_:Int)(1)( (x,y) => x * y)
    val general_sum = general(x => x)(_:Int,_:Int)(0)( (x,y) => x + y)

    println(general_product(2,5)) // 120
    println(general_sum(2,5)) // 14

    val productMap = mapReduce(x => x, (x,y) => x * y, 1)(_: Int,_: Int)
    println(productMap(2,5)) // 120

  }

  def product(f: Int => Int)(a: Int, b: Int): Int = 
    if (a > b) 1 else f(a) * product(f)(a + 1, b)
  
    
  def factorial(n: Int): Int =
    product(x=>x)(1, n)

  /**
   * Эта общая функция принимает f1 - функцию воздействия как и в примерах раньше,
   * параметры a и b как интервалы как в примерах раньше.
   * 
   * u - это "Unit"-значение, для суммы это 0, а для умножения - 1. Его мы 
   * возвращаем когда доходим до точки конца рекурсии.
   * 
   * f2 - это действие над числами. Сумма или умножение, как в примере выше.
   * */
  def general(f1: Int => Int)(a: Int, b: Int)(u: Int)(f2: (Int, Int) => Int ): Int = 
    if (a > b) u else f2( f1(a), general(f1)(a + 1, b)(u)(f2) )



  def mapReduce(f: Int => Int, combine: (Int, Int) => Int, zero: Int)(a: Int, b: Int): Int =
    if (a > b) zero
    else combine(f(a), mapReduce(f, combine, zero)(a + 1, b))
}