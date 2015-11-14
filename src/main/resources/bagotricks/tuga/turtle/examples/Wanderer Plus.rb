def mark
	color black
	turn left
	jump 20
	turn around
	walk 40
	turn around
	jump 20
	turn right
end

def update_path_color
	3.times do |i|
		component = @path_color[i] + spin(4)
		@path_color[i] = [[0, component].max, 100].min
	end
	color @path_color
end

def spin(extent)
	2 * extent * (rand - 0.5)
end

@path_color = [25, 25, 25]

1001.times do |i|
	update_path_color
	mark if (i % 100).zero?
	turn spin(30)
	walk 3
end
