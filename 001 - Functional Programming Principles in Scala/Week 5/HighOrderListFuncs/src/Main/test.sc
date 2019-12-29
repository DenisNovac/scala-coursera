val l = List("apple", "juice", "abc", "john", "mary", "vodka")
l.filter(x => x.length < 5)
l filterNot (x => x.contains("a"))
l partition(x => x.length <5)

l takeWhile(x => x.contains("a"))
l dropWhile(x => x.contains("a"))


l span(x => x.contains("a"))
