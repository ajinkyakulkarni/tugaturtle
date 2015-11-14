def main
	spiral 500, 1200, 91
end

def spiral(lines, size, angle)
	2.times do
		turn right
		jump 0.5 * size
	end
	turn 180
	distance = (1.0 / lines) * size
	lines.downto 1 do |i|
		walk distance * i
		turn angle
	end
end

### Call main ###
main
