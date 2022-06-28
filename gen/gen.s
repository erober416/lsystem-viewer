.text
#if(__APPLE__)
	.global _entry_point

_entry_point:
#else
	.global entry_point

entry_point:
#endif
	push %rbp	# save stack frame for C convention
	mov %rsp, %rbp

	pushq %rbx
	pushq %r12
	pushq %r13
	pushq %r14
	pushq %r15

	# beginning generated code
	movq $3, %rbx
	movq $5, %rcx
	cmp %rcx, %rbx
	jg if1_then
	movq $4, %rbx
	jmp if1_end
if1_then:
	movq $9, %rbx
	movq $0, %rcx
	cmp %rcx, %rbx
	jl if2_then
	movq $2, %rbx
	jmp if2_end
if2_then:
	movq $1, %rbx
if2_end:
if1_end:
	movq %rbx, %rax
	# end generated code
	# %rax contains the result

	popq %r15
	popq %r14
	popq %r13
	popq %r12
	popq %rbx
	mov %rbp, %rsp	# reset frame
	pop %rbp
	ret



