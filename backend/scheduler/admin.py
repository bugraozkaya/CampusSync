from django.contrib import admin
from .models import Institution, User, Department, Semester, Classroom, Course, Schedule

admin.site.register(Institution)
admin.site.register(User)
admin.site.register(Department)
admin.site.register(Semester)
admin.site.register(Classroom)
admin.site.register(Course)
admin.site.register(Schedule)