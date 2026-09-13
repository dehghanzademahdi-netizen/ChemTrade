from pathlib import Path

p = Path("app/src/main/java/com/dehghanzadeh/chemtrade/EntryActivity.kt")
s = p.read_text(encoding="utf-8")
s = s.replace('Button(next, Modifier.fillMaxWidth().height(56.dp), RoundedCornerShape(18.dp))', 'Button(onClick = next, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp))')
s = s.replace('Button(next, Modifier.fillMaxWidth().height(52.dp), RoundedCornerShape(16.dp))', 'Button(onClick = next, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp))')
s = s.replace('Button(verify, Modifier.fillMaxWidth().height(56.dp), RoundedCornerShape(18.dp))', 'Button(onClick = verify, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp))')
s = s.replace('Button(save, Modifier.fillMaxWidth().height(56.dp), RoundedCornerShape(18.dp))', 'Button(onClick = save, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp))')
p.write_text(s, encoding="utf-8")
print("COMPOSE BUTTON ARGUMENTS FIXED")
