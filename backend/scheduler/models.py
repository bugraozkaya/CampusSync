from django.db import models
from django.contrib.auth.models import AbstractUser

class Institution(models.Model):
    name = models.CharField(max_length=255, verbose_name="Kurum Adı")
    institution_type = models.CharField(max_length=50, verbose_name="Kurum Tipi")
    created_at = models.DateTimeField(auto_now_add=True)
    def __str__(self): return self.name

class User(AbstractUser):
    ROLE_CHOICES = (
        ('SUPERADMIN', 'Sistem Yöneticisi'),
        ('ADMIN', 'Kurum Yöneticisi (IT)'),
        ('LECTURER', 'Öğretim Görevlisi'),
        ('STUDENT', 'Öğrenci'),
    )
    institution = models.ForeignKey(Institution, on_delete=models.CASCADE, null=True, blank=True)
    role = models.CharField(max_length=20, choices=ROLE_CHOICES, default='STUDENT')
    is_approved = models.BooleanField(default=False)

class Department(models.Model):
    institution = models.ForeignKey(Institution, on_delete=models.CASCADE)
    name = models.CharField(max_length=255)
    def __str__(self): return f"{self.name} ({self.institution.name})"

class Semester(models.Model):
    institution = models.ForeignKey(Institution, on_delete=models.CASCADE)
    name = models.CharField(max_length=100)
    is_active = models.BooleanField(default=False)

class Classroom(models.Model):
    institution = models.ForeignKey(Institution, on_delete=models.CASCADE)
    name = models.CharField(max_length=100)
    capacity = models.IntegerField(null=True, blank=True)

class Course(models.Model):
    department = models.ForeignKey(Department, on_delete=models.CASCADE)
    lecturer = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, limit_choices_to={'role': 'LECTURER'})
    course_code = models.CharField(max_length=20)
    course_name = models.CharField(max_length=255)

class Schedule(models.Model):
    course = models.ForeignKey(Course, on_delete=models.CASCADE)
    semester = models.ForeignKey(Semester, on_delete=models.CASCADE)
    classroom = models.ForeignKey(Classroom, on_delete=models.SET_NULL, null=True)
    day_of_week = models.CharField(max_length=3)
    start_time = models.TimeField()
    end_time = models.TimeField()

# --- YENİ: Hoca Müsaitlik Durumu ---
class Unavailability(models.Model):
    lecturer = models.ForeignKey(User, on_delete=models.CASCADE, limit_choices_to={'role': 'LECTURER'})
    day = models.CharField(max_length=20) # Pazartesi, Salı...
    hour = models.CharField(max_length=10) # 09:00, 10:00...

    class Meta:
        unique_together = ('lecturer', 'day', 'hour') # Aynı hoca aynı saati iki kez ekleyemesin
