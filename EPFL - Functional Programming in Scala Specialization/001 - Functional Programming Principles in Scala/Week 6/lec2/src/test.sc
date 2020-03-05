object pairs {
  val n = 7
  val vectors = (1 until n) map (i =>
    (1 until i) map (j => (i,j)))

  val vectorFlatten = vectors.flatten


  val vectorsFlat = (1 until n) flatMap (i =>
    (1 until i) map (j => (i,j)))

  def isPrime(n: Int): Boolean =
    !(2 until n).exists(i => (n>0) & (n%i)==0)

  vectorsFlat filter (pair => isPrime(pair._1 + pair._2))

  for (i <- 1 to 10 if i>8) yield i
  (1 to 10) filter (i => i>8)

  case class Person(name: String, age: Int)

  val persons = List(Person("John", 22), Person("Sam", 43), Person("Lena", 23), Person("James", 31))
  for (p <- persons if p.age < 30) yield p.name
  persons filter(p => p.age < 30) map (p => p.name)

  for {
    p <- persons
    if p.age < 30
    if p.name != "John"
  } yield p.name



  for {
    i <- 1 until n
    j <- 1 until i
    if isPrime(i + j)
  } yield (i,j)



  def scalarProduct(xs: Vector[Double], ys:Vector[Double]): Double =
    (xs zip ys).map{ case (x,y) => x * y}.sum

  /*def scalarProduct(xs: Vector[Double], ys: Vector[Double]): Double =
    (xs zip ys).map(xy => xy._1 * xy._2).sum*/

  scalarProduct(Vector(1,2), Vector(3,4))

  def scalarProduct2(xs: Vector[Double], ys:Vector[Double]): Double =
    (for {
      xy <- xs zip ys
    } yield xy._1 * xy._2 ).sum

  scalarProduct2(Vector(1,2), Vector(3,4))

}

