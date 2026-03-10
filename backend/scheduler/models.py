from django.db import models
from django.contrib.auth.models import AbstractUser

# 1. Kurumlar Tablosu
class Institution(models.Model):
    name = models.CharField(max_length=255, verbose_name="Kurum Adı")
    institution_type = models.CharField(max_length=50, verbose_name="Kurum Tipi") # Üniversite, Lise vb.
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self):
        return self.name

# 2. Gelişmiş Kullanıcı Tablosu (Django'nun standart kullanıcısını eziyoruz)
class User(AbstractUser):
    ROLE_CHOICES = (
        ('SUPERADMIN', 'Sistem Yöneticisi'),
        ('ADMIN', 'Kurum Yöneticisi (IT)'),
        ('LECTURER', 'Öğretim Görevlisi'),
        ('STUDENT', 'Öğrenci'),
    )
    institution = models.ForeignKey(Institution, on_delete=models.CASCADE, null=True, blank=True, verbose_name="Bağlı Olduğu Kurum")
    role = models.CharField(max_length=20, choices=ROLE_CHOICES, default='STUDENT', verbose_name="Kullanıcı Rolü")
    is_approved = models.BooleanField(default=False, verbose_name="Admin Onayı")

    def __str__(self):
        return f"{self.get_full_name() or self.username} - {self.get_role_display()}"

# 3. Bölümler Tablosu
class Department(models.Model):
    institution = models.ForeignKey(Institution, on_delete=models.CASCADE)
    name = models.CharField(max_length=255, verbose_name="Bölüm Adı")

    def __str__(self):
        return f"{self.name} ({self.institution.name})"

# 4. Dönemler Tablosu
class Semester(models.Model):
    institution = models.ForeignKey(Institution, on_delete=models.CASCADE)
    name = models.CharField(max_length=100, verbose_name="Dönem Adı") # Örn: 2025-2026 Güz
    start_date = models.DateField(verbose_name="Başlangıç Tarihi")
    end_date = models.DateField(verbose_name="Bitiş Tarihi")
    is_active = models.BooleanField(default=False, verbose_name="Aktif Dönem mi?")

    def __str__(self):
        return f"{self.name} - {self.institution.name}"

# 5. Derslikler Tablosu
class Classroom(models.Model):
    institution = models.ForeignKey(Institution, on_delete=models.CASCADE)
    name = models.CharField(max_length=100, verbose_name="Derslik/Amfi Adı")
    capacity = models.IntegerField(null=True, blank=True, verbose_name="Kapasite")

    def __str__(self):
        return f"{self.name} ({self.institution.name})"

# 6. Dersler Tablosu
class Course(models.Model):
    department = models.ForeignKey(Department, on_delete=models.CASCADE)
    lecturer = models.ForeignKey(User, on_delete=models.SET_NULL, null=True, limit_choices_to={'role': 'LECTURER'}, verbose_name="Öğretim Görevlisi")
    course_code = models.CharField(max_length=20, verbose_name="Ders Kodu") # Örn: CNG 103
    course_name = models.CharField(max_length=255, verbose_name="Ders Adı")

    def __str__(self):
        return f"{self.course_code} - {self.course_name}"

# 7. Ders Programı (Takvim) Tablosu
class Schedule(models.Model):
    DAY_CHOICES = (
        ('MON', 'Pazartesi'),
        ('TUE', 'Salı'),
        ('WED', 'Çarşamba'),
        ('THU', 'Perşembe'),
        ('FRI', 'Cuma'),
        ('SAT', 'Cumartesi'),
        ('SUN', 'Pazar'),
    )
    course = models.ForeignKey(Course, on_delete=models.CASCADE)
    semester = models.ForeignKey(Semester, on_delete=models.CASCADE)
    classroom = models.ForeignKey(Classroom, on_delete=models.SET_NULL, null=True)
    day_of_week = models.CharField(max_length=3, choices=DAY_CHOICES, verbose_name="Gün")
    start_time = models.TimeField(verbose_name="Başlangıç Saati")
    end_time = models.TimeField(verbose_name="Bitiş Saati")

    def __str__(self):
        return f"{self.course.course_code} | {self.get_day_of_week_display()} | {self.start_time.strftime('%H:%M')}-{self.end_time.strftime('%H:%M')}"