def house size
	size *= 0.6
	half = size / 2

	# main box
	turn left
	jump half
	turn right
	square size
	jump size

	# roof
	turn -45
	roof = Math.sqrt(half * half + half * half)
	walk roof
	turn right
	walk roof
	turn -45

	# door
	third = size / 3
	jump size
	turn right
	jump 2 * third
	turn right
	rectangle half, third
end

def rectangle length, width
	2.times do
		walk length
		turn right
		walk width
		turn right
	end
end

def square size
	rectangle size, size
end

house 900
